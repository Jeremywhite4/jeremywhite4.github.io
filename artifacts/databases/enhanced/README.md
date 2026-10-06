# Enhanced Animal Shelter Dashboard — CS-499 Enhancement Three (Databases)

Enhancement of the CS-340 *Grazioso Salvare* Animal Shelter artifact
(Austin Animal Center data). The original artifact was a minimal PyMongo CRUD
wrapper with hardcoded connection details and a Dash dashboard. This enhancement
raises the data layer to a performant, secure, and testable component and adds a
new aggregation-driven dashboard view.

## What changed vs. the original

| Area | Original | Enhanced |
|------|----------|----------|
| Connection | host/port/db/collection **hardcoded** in `animal_shelter.py` | Read from environment / `ShelterConfig`; credentials via `.env`, never in source |
| Errors | none — bad connection failed late | `serverSelectionTimeoutMS` + explicit `ConnectionError` on connect |
| Security | raw query passed straight to Mongo | Query **sanitization** rejects `$where`/`$expr`/`$function`/`$out`/etc.; empty-filter delete refused |
| Indexing | none | `ensure_indexes()` builds `animal_type`, `breed`, and compound `outcome_type+datetime`; `explain_query()` shows the plan |
| Aggregation | none | `aggregate()` + `outcomes_by_type(start,end)` pipeline powering a new bar-chart view |
| Pagination | `read()` returned everything | `read_page()` returns bounded pages + metadata (stretch goal) |
| Tests | none | `pytest` suite (mongomock) covering CRUD, sanitization, indexing, aggregation, pagination |
| Least privilege | app used broad user | documented least-privilege `aacuser` (readWrite on `AAC` only) |

## Course outcomes
- **Outcome 3** — designing/evaluating a data solution with measured trade-offs (index vs. collection scan via `explain()`).
- **Outcome 4** — well-founded database tools/techniques (indexes, aggregation pipeline, pagination).
- **Outcome 5** — securing data (credentials out of source, least privilege, input sanitization against operator injection).
- **Outcome 2** — communicating results through clear visualizations and this documentation.

## Setup

```bash
python -m venv .venv
# Windows: .venv\Scripts\activate   |   macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env      # then edit .env with real credentials
```

### Least-privilege database user
Create an application user scoped to the `AAC` database only:

```javascript
use AAC
db.createUser({
  user: "aacuser",
  pwd:  "<strong-password>",
  roles: [ { role: "readWrite", db: "AAC" } ]   // no admin / no other DBs
})
```

## Running the tests

```bash
pytest -q
```

The tests use an in-memory `mongomock` client, so **no live MongoDB is required.**

## Running the dashboard notebook

Open `ProjectTwoDashboard_Enhanced.ipynb` in Jupyter or Google Colab and
**Run All**. The notebook:

1. Loads `.env` (if present) and tries to connect to the configured MongoDB.
2. If no MongoDB is reachable, it **falls back to an in-memory `mongomock`** seeded
   from `datasets/aac_shelter_outcomes.csv` (or a small synthetic dataset if the
   CSV is absent), so it always runs end-to-end.
3. Builds the indexes, prints `explain()` evidence (COLLSCAN → IXSCAN), runs the
   aggregation, and launches the enhanced Dash app with the new
   "Outcomes by Animal Type" chart.

## Files
- `animal_shelter.py` — enhanced data-access layer (CRUD + indexing + aggregation + sanitization + pagination).
- `data_loader.py` — CSV/synthetic seeding helper (works with real Mongo or mongomock).
- `test_animal_shelter.py` — pytest suite (mongomock).
- `ProjectTwoDashboard_Enhanced.ipynb` — self-contained enhanced dashboard.
- `requirements.txt`, `.env.example` — dependencies and configuration template.
