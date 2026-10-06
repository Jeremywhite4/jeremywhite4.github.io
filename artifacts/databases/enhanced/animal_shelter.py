"""Enhanced CRUD data-access layer for the Austin Animal Center (AAC) MongoDB collection.

CS-499 Enhancement Three (Databases). This module refactors the original CS-340
``AnimalShelter`` class from a minimal, insecure CRUD wrapper into a performant,
secure, and testable data component. The enhancements, mapped to the Module One
plan, are:

1. Configuration & security (Outcome 5): connection details and credentials are
   read from the environment / a config object instead of being hardcoded in
   source. The connection uses ``serverSelectionTimeoutMS`` and surfaces a clear
   :class:`ConnectionError` on failure, and the README documents connecting with
   a least-privilege database user.
2. Indexing (Outcomes 3/4): :meth:`ensure_indexes` creates single-field indexes on
   ``animal_type`` and ``breed`` and a compound index on
   ``outcome_type + datetime``; :meth:`explain_query` exposes the query plan so the
   index win can be shown with ``explain()`` output.
3. Aggregation (Outcomes 3/4/2): :meth:`aggregate` runs arbitrary (validated)
   pipelines and :meth:`outcomes_by_type` implements a concrete
   "outcomes by animal type over a date range" pipeline that powers a new
   dashboard view.
4. Validation & sanitization (Outcome 5): :meth:`read` rejects query documents that
   contain unexpected MongoDB operators (a ``$where`` / operator-injection guard),
   and every public method validates its inputs.
5. Pagination (stretch, Outcome 3): :meth:`read_page` returns bounded result pages so
   large result sets do not overwhelm the client or time out.

The class accepts an injected ``client`` so it can be exercised against an
in-memory ``mongomock`` client in unit tests and in the notebook's offline mode,
with no code path difference from a real ``pymongo`` client.
"""

from __future__ import annotations

import os
from typing import Any, Dict, Iterable, List, Optional

try:
    # pymongo is the production driver. It is optional at import time so the
    # module can be imported in environments that only have mongomock.
    from pymongo import ASCENDING, MongoClient
    from pymongo.errors import ConnectionFailure, PyMongoError, ServerSelectionTimeoutError
except ImportError:  # pragma: no cover - exercised only where pymongo is absent
    MongoClient = None  # type: ignore[assignment]
    ASCENDING = 1  # type: ignore[assignment]

    class PyMongoError(Exception):
        """Fallback base error when pymongo is not installed."""

    class ConnectionFailure(PyMongoError):
        """Fallback connection error."""

    class ServerSelectionTimeoutError(PyMongoError):
        """Fallback server-selection error."""


# Operators that must never appear in a client-supplied query filter. ``$where``
# and ``$expr`` allow arbitrary JavaScript / expression evaluation and are the
# classic NoSQL-injection vector; ``$function``/``$accumulator`` allow server-side
# code. Rejecting them keeps untrusted filter input from executing code.
_DISALLOWED_OPERATORS = frozenset({"$where", "$expr", "$function", "$accumulator", "$merge", "$out"})

# Bound on a single page so a caller cannot request an unbounded result set.
_MAX_PAGE_SIZE = 500


class AnimalShelterError(Exception):
    """Base exception for the data-access layer."""


class ConnectionError(AnimalShelterError):  # noqa: A001 - intentional domain name
    """Raised when a connection to MongoDB cannot be established."""


class ValidationError(AnimalShelterError):
    """Raised when a query or input fails validation / sanitization."""


class ShelterConfig:
    """Connection configuration resolved from explicit args or the environment.

    Precedence for each field: explicit constructor argument > environment
    variable > safe default. Credentials have *no* default and must be supplied
    via ``AAC_USER`` / ``AAC_PASS`` (or the ``username``/``password`` arguments)
    so they never live in source control.
    """

    def __init__(
        self,
        username: Optional[str] = None,
        password: Optional[str] = None,
        host: Optional[str] = None,
        port: Optional[int] = None,
        db: Optional[str] = None,
        collection: Optional[str] = None,
        auth_source: Optional[str] = None,
    ) -> None:
        self.username = username or os.getenv("AAC_USER")
        self.password = password or os.getenv("AAC_PASS")
        self.host = host or os.getenv("AAC_HOST", "localhost")
        self.port = int(port or os.getenv("AAC_PORT", "27017"))
        self.db = db or os.getenv("AAC_DB", "AAC")
        self.collection = collection or os.getenv("AAC_COL", "animals")
        self.auth_source = auth_source or os.getenv("AAC_AUTHSOURCE", self.db)

    def build_uri(self) -> str:
        """Build a MongoDB URI, including credentials only when both are present."""
        if self.username and self.password:
            from urllib.parse import quote_plus

            user = quote_plus(self.username)
            pwd = quote_plus(self.password)
            return f"mongodb://{user}:{pwd}@{self.host}:{self.port}/?authSource={self.auth_source}"
        return f"mongodb://{self.host}:{self.port}/"


class AnimalShelter:
    """CRUD + indexing + aggregation operations for the AAC ``animals`` collection.

    Parameters
    ----------
    username, password, host, port, db, collection:
        Optional connection overrides. When omitted they fall back to the
        corresponding environment variables (see :class:`ShelterConfig`).
    client:
        An optional pre-built MongoClient-compatible object. Injecting a
        ``mongomock.MongoClient`` here lets the class run fully in-memory (used by
        the unit tests and the notebook's offline mode) with no other changes.
    config:
        An optional :class:`ShelterConfig`. When provided it takes precedence over
        the individual keyword arguments.
    """

    def __init__(
        self,
        username: Optional[str] = None,
        password: Optional[str] = None,
        host: Optional[str] = None,
        port: Optional[int] = None,
        db: Optional[str] = None,
        collection: Optional[str] = None,
        client: Any = None,
        config: Optional[ShelterConfig] = None,
    ) -> None:
        self.config = config or ShelterConfig(
            username=username,
            password=password,
            host=host,
            port=port,
            db=db,
            collection=collection,
        )

        if client is not None:
            # Dependency-injected client (e.g. mongomock) - no network access.
            self.client = client
        else:
            if MongoClient is None:  # pragma: no cover
                raise ConnectionError(
                    "pymongo is not installed and no client was injected. "
                    "Install pymongo or pass a client (e.g. mongomock.MongoClient())."
                )
            try:
                self.client = MongoClient(
                    self.config.build_uri(),
                    serverSelectionTimeoutMS=5000,
                )
                # Force server selection now so failures surface here, not later.
                self.client.admin.command("ping")
            except (ConnectionFailure, ServerSelectionTimeoutError) as exc:
                raise ConnectionError(
                    f"Could not connect to MongoDB at {self.config.host}:{self.config.port}: {exc}"
                ) from exc

        self.database = self.client[self.config.db]
        self.collection = self.database[self.config.collection]

    # ------------------------------------------------------------------ #
    # Validation helpers
    # ------------------------------------------------------------------ #
    @staticmethod
    def _validate_document(value: Any, *, disallow_operators: bool) -> Dict[str, Any]:
        """Ensure ``value`` is a dict and, optionally, free of disallowed operators.

        Recursively walks nested dicts/lists so an injection attempt hidden inside
        a nested value (e.g. ``{"a": {"$where": "..."}}``) is still rejected.
        """
        if not isinstance(value, dict):
            raise ValidationError("Query/data must be a dictionary.")

        if disallow_operators:
            AnimalShelter._scan_for_disallowed(value)
        return value

    @staticmethod
    def _scan_for_disallowed(node: Any) -> None:
        if isinstance(node, dict):
            for key, sub in node.items():
                if isinstance(key, str) and key in _DISALLOWED_OPERATORS:
                    raise ValidationError(f"Disallowed operator in query: {key}")
                AnimalShelter._scan_for_disallowed(sub)
        elif isinstance(node, (list, tuple)):
            for item in node:
                AnimalShelter._scan_for_disallowed(item)

    # ------------------------------------------------------------------ #
    # Indexing (enhancement 2)
    # ------------------------------------------------------------------ #
    def ensure_indexes(self) -> List[str]:
        """Create the indexes that back the common dashboard queries.

        Idempotent: MongoDB (and mongomock) return the existing index when the
        same specification is requested again. Returns the list of index names.
        """
        names = [
            self.collection.create_index([("animal_type", ASCENDING)], name="idx_animal_type"),
            self.collection.create_index([("breed", ASCENDING)], name="idx_breed"),
            self.collection.create_index(
                [("outcome_type", ASCENDING), ("datetime", ASCENDING)],
                name="idx_outcome_datetime",
            ),
        ]
        return names

    def list_indexes(self) -> List[str]:
        """Return the names of all indexes currently on the collection."""
        return [ix["name"] for ix in self.collection.list_indexes()]

    def explain_query(self, query: Dict[str, Any]) -> Dict[str, Any]:
        """Return the query planner ``explain()`` output for a find filter.

        Used in the notebook to show that an indexed field yields an ``IXSCAN``
        stage instead of a full ``COLLSCAN``. ``mongomock`` implements a subset of
        explain, so callers should treat a missing key defensively.
        """
        self._validate_document(query, disallow_operators=True)
        # mongomock cursors do not implement .explain(); fall back gracefully so
        # the notebook's offline mode still produces a (clearly labelled) result.
        try:
            cursor = self.collection.find(query)
            explain = getattr(cursor, "explain", None)
            if not callable(explain):
                return {"explainError": "explain() not supported by this client (offline/mongomock)."}
            return explain()
        except (PyMongoError, NotImplementedError, AttributeError) as exc:
            return {"explainError": str(exc)}

    # ------------------------------------------------------------------ #
    # CRUD (enhanced with validation)
    # ------------------------------------------------------------------ #
    def create(self, data: Dict[str, Any]) -> bool:
        """Insert one document. Returns ``True`` when acknowledged."""
        self._validate_document(data, disallow_operators=False)
        if not data:
            raise ValidationError("Nothing to save: data is empty.")
        result = self.collection.insert_one(dict(data))
        return bool(result.acknowledged)

    def read(self, query: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """Return all documents matching a sanitized ``query`` (``_id`` suppressed).

        A ``None`` query is treated as "match everything" (``{}``) rather than an
        error, which is friendlier for the dashboard's reset filter. Any query
        containing a disallowed operator raises :class:`ValidationError`.
        """
        if query is None:
            query = {}
        self._validate_document(query, disallow_operators=True)
        cursor = self.collection.find(query, {"_id": False})
        return list(cursor)

    def read_page(
        self,
        query: Optional[Dict[str, Any]] = None,
        page: int = 1,
        page_size: int = 50,
        sort: Optional[Iterable] = None,
    ) -> Dict[str, Any]:
        """Return one bounded page of results plus pagination metadata.

        Parameters
        ----------
        page:
            1-based page number.
        page_size:
            Number of documents per page, clamped to ``[1, _MAX_PAGE_SIZE]``.
        sort:
            Optional iterable of ``(field, direction)`` pairs.
        """
        if query is None:
            query = {}
        self._validate_document(query, disallow_operators=True)
        if page < 1:
            raise ValidationError("page must be >= 1.")
        page_size = max(1, min(int(page_size), _MAX_PAGE_SIZE))

        total = self.collection.count_documents(query)
        cursor = self.collection.find(query, {"_id": False})
        if sort:
            cursor = cursor.sort(list(sort))
        cursor = cursor.skip((page - 1) * page_size).limit(page_size)
        records = list(cursor)
        return {
            "page": page,
            "page_size": page_size,
            "total": total,
            "total_pages": (total + page_size - 1) // page_size if page_size else 0,
            "records": records,
        }

    def update(self, query: Dict[str, Any], new_values: Dict[str, Any]) -> int:
        """Apply ``$set`` of ``new_values`` to all matches. Returns modified count."""
        self._validate_document(query, disallow_operators=True)
        self._validate_document(new_values, disallow_operators=False)
        if not new_values:
            raise ValidationError("Nothing to update: new_values is empty.")
        result = self.collection.update_many(query, {"$set": new_values})
        return int(result.modified_count)

    def delete(self, query: Dict[str, Any]) -> int:
        """Delete all documents matching ``query``. Returns deleted count."""
        self._validate_document(query, disallow_operators=True)
        if not query:
            # Refuse an empty filter so a bug cannot wipe the whole collection.
            raise ValidationError("Refusing to delete with an empty query.")
        result = self.collection.delete_many(query)
        return int(result.deleted_count)

    # ------------------------------------------------------------------ #
    # Aggregation (enhancement 3)
    # ------------------------------------------------------------------ #
    def aggregate(self, pipeline: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Run a validated aggregation pipeline and return the results."""
        if not isinstance(pipeline, list) or not all(isinstance(s, dict) for s in pipeline):
            raise ValidationError("pipeline must be a list of stage dictionaries.")
        for stage in pipeline:
            # Guard against $out/$merge (write-out) and code operators in stages.
            self._scan_for_disallowed(stage)
        return list(self.collection.aggregate(pipeline))

    def outcomes_by_type(
        self,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """Aggregate outcome counts grouped by animal type over a date range.

        Powers the new dashboard bar-chart view. ``start_date``/``end_date`` are
        ISO date strings compared against the string ``datetime`` field, matching
        how the AAC data stores timestamps (e.g. ``"2017-04-11 09:00:00"``).
        Returns a list of ``{animal_type, outcome_type, count}`` documents.
        """
        match: Dict[str, Any] = {}
        if start_date or end_date:
            rng: Dict[str, Any] = {}
            if start_date:
                rng["$gte"] = start_date
            if end_date:
                rng["$lte"] = end_date
            match["datetime"] = rng

        pipeline: List[Dict[str, Any]] = []
        if match:
            pipeline.append({"$match": match})
        pipeline.extend(
            [
                {
                    "$group": {
                        "_id": {"animal_type": "$animal_type", "outcome_type": "$outcome_type"},
                        "count": {"$sum": 1},
                    }
                },
                {
                    "$project": {
                        "_id": False,
                        "animal_type": "$_id.animal_type",
                        "outcome_type": "$_id.outcome_type",
                        "count": True,
                    }
                },
                {"$sort": {"count": -1}},
            ]
        )
        return self.aggregate(pipeline)

    def close(self) -> None:
        """Close the underlying client connection if it supports it."""
        close = getattr(self.client, "close", None)
        if callable(close):
            close()
