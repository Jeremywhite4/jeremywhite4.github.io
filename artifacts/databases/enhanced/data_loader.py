"""CSV -> MongoDB seeding helper for the AAC dashboard.

Keeps ingestion out of the notebook and out of the CRUD class so the data-access
layer stays focused. Works against either a real pymongo collection or an
injected mongomock collection, which is what makes the notebook runnable offline.
"""

from __future__ import annotations

import csv
import io
import os
from typing import Any, Dict, Iterator, List, Optional

# Fields that should be stored as floats rather than strings.
_FLOAT_FIELDS = ("location_lat", "location_long", "age_upon_outcome_in_weeks")


def _coerce(row: Dict[str, str]) -> Dict[str, Any]:
    """Coerce known numeric columns to float; leave the rest as strings."""
    doc: Dict[str, Any] = dict(row)
    for field in _FLOAT_FIELDS:
        raw = doc.get(field, "")
        if raw not in (None, ""):
            try:
                doc[field] = float(raw)
            except (TypeError, ValueError):
                doc[field] = None
        else:
            doc[field] = None
    return doc


def iter_csv_documents(csv_path: str) -> Iterator[Dict[str, Any]]:
    """Yield one coerced document per CSV row."""
    with io.open(csv_path, "r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            yield _coerce(row)


def load_csv_into_collection(collection: Any, csv_path: str, drop_existing: bool = True) -> int:
    """Bulk-load ``csv_path`` into ``collection``. Returns number inserted."""
    if drop_existing:
        collection.delete_many({})
    docs: List[Dict[str, Any]] = list(iter_csv_documents(csv_path))
    if docs:
        collection.insert_many(docs)
    return len(docs)


# A tiny synthetic fallback so the notebook still runs if the CSV is absent.
SYNTHETIC_DOCS: List[Dict[str, Any]] = [
    {"rec_num": 1, "animal_type": "Dog", "breed": "Labrador Retriever Mix", "color": "Black",
     "name": "Rex", "outcome_type": "Adoption", "sex_upon_outcome": "Intact Female",
     "datetime": "2017-01-05 10:00:00", "age_upon_outcome_in_weeks": 52.0,
     "location_lat": 30.75, "location_long": -97.48},
    {"rec_num": 2, "animal_type": "Dog", "breed": "German Shepherd", "color": "Tan",
     "name": "Bud", "outcome_type": "Transfer", "sex_upon_outcome": "Intact Male",
     "datetime": "2017-02-11 09:00:00", "age_upon_outcome_in_weeks": 80.0,
     "location_lat": 30.51, "location_long": -97.34},
    {"rec_num": 3, "animal_type": "Cat", "breed": "Domestic Shorthair Mix", "color": "Black/White",
     "name": "Milo", "outcome_type": "Adoption", "sex_upon_outcome": "Neutered Male",
     "datetime": "2017-03-01 12:00:00", "age_upon_outcome_in_weeks": 30.0,
     "location_lat": 30.44, "location_long": -97.61},
    {"rec_num": 4, "animal_type": "Dog", "breed": "Newfoundland", "color": "Brown",
     "name": "Bear", "outcome_type": "Adoption", "sex_upon_outcome": "Intact Female",
     "datetime": "2017-07-04 08:30:00", "age_upon_outcome_in_weeks": 60.0,
     "location_lat": 30.62, "location_long": -97.70},
    {"rec_num": 5, "animal_type": "Cat", "breed": "Siamese Mix", "color": "Seal",
     "name": "Luna", "outcome_type": "Return to Owner", "sex_upon_outcome": "Spayed Female",
     "datetime": "2017-06-01 12:00:00", "age_upon_outcome_in_weeks": 45.0,
     "location_lat": 30.39, "location_long": -97.55},
]


def load_synthetic_into_collection(collection: Any, drop_existing: bool = True) -> int:
    """Load the small synthetic dataset. Returns number inserted."""
    if drop_existing:
        collection.delete_many({})
    collection.insert_many([dict(d) for d in SYNTHETIC_DOCS])
    return len(SYNTHETIC_DOCS)


def resolve_csv_path(explicit: Optional[str] = None) -> Optional[str]:
    """Find the dataset CSV from an explicit path, env var, or common locations."""
    candidates = [
        explicit,
        os.getenv("AAC_CSV"),
        os.path.join("datasets", "aac_shelter_outcomes.csv"),
        "aac_shelter_outcomes.csv",
    ]
    for path in candidates:
        if path and os.path.exists(path):
            return path
    return None
