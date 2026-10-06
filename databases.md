---
layout: default
title: Databases - Animal Shelter Dashboard
---

# Databases

### Artifact: Animal Shelter Dashboard (CS-340 - Python / Dash / MongoDB)

| | |
|---|---|
| **Original course** | CS-340 Advanced Programming Concepts (June 2026) |
| **Language / stack** | Python, Dash / Plotly, MongoDB (PyMongo) |
| **Category** | Databases (Milestone Four) |

**Code:**
[Original artifact](artifacts/databases/original/) &nbsp;|&nbsp;
[Enhanced artifact](artifacts/databases/enhanced/) &nbsp;|&nbsp;
[Enhanced README](artifacts/databases/enhanced/README.md) &nbsp;|&nbsp;
[Enhancement narrative (Word)](narratives/CS499_Enhancement3_Narrative_Jeremy_White.docx)

[&larr; Back to portfolio home](index.md)

---

### What changed

| Area | Original | Enhanced |
|------|----------|----------|
| Connection | Host / port / db **hardcoded** in source | Read from environment / `.env`; **credentials out of source** |
| Error handling | None (failed late) | `serverSelectionTimeoutMS` + explicit `ConnectionError` |
| Security | Raw query passed straight to Mongo | **Sanitization** rejects `$where`/`$expr`/etc.; empty-filter delete refused |
| Indexing | None | `animal_type`, `breed`, compound `outcome_type+datetime`; `explain()` evidence (COLLSCAN &rarr; IXSCAN) |
| Aggregation | None | Pipeline (`outcomes_by_type`) powering a **new bar-chart view** |
| Pagination | `read()` returned everything | Bounded `read_page()` with metadata |
| Tests | None | 18-test `pytest` suite (mongomock, no live DB needed) |
| Access | Broad user | Documented **least-privilege** `aacuser` (readWrite on `AAC` only) |

---

### Description of the Artifact and Justification for Its Inclusion

The artifact I selected for the Databases category is the Grazioso Salvare Animal Shelter Dashboard, which I originally built as Project Two for CS-340: Advanced Programming Concepts in 2024. It is a Python application that visualizes the Austin Animal Center (AAC) outcomes dataset. The original artifact has two parts: animal_shelter.py, a CRUD module that wraps a MongoDB collection using PyMongo, and ProjectTwoDashboard.ipynb, a Dash/Plotly dashboard that reads from that module and presents an interactive data table, a breed pie chart, and a geolocation map, with radio buttons that filter the animals to the breeds Grazioso Salvare uses for water, mountain, and disaster rescue work. I chose this artifact because it is the clearest database piece in my body of coursework and because the original data layer, while functional, was written the way a student writes a first database wrapper: it worked against the one machine it was built on and stopped there. The AnimalShelter class hardcoded the host, port, database name, and collection directly in the source; it accepted a raw query dictionary and passed it straight to MongoDB with no validation; it created no indexes, so every filter was a full collection scan; it had no aggregation, no pagination, and no tests. Those are exactly the gaps that separate a class-project data component from one you would put into production, which made the artifact an honest opportunity to demonstrate real database skill rather than cosmetic polish. The components that best showcase my skills are the AnimalShelter data-access layer itself, which I re-engineered from a thin CRUD wrapper into a configurable, indexed, secured, and tested component, and the new aggregation-driven view I added to the dashboard, which turns a MongoDB aggregation pipeline into a business-facing chart. The improvement is not a rewrite of what the dashboard does* for its user; it is a substantial strengthening of how the data layer beneath it behaves.

### How I Improved the Artifact

I performed the planned enhancement across the five areas from my Module One plan, and, as in my earlier enhancements, I documented every file with a header explaining its intent and mapped each change back to the outcome it serves. First, I moved configuration and credentials out of the source. The connection details are now resolved by a ShelterConfig class that reads environment variables (with an .env.example template and python-dotenv support) and falls back to safe defaults, so no host or password lives in version control. The constructor forces server selection at connect time with a five-second timeout and raises an explicit ConnectionError on failure instead of failing late on the first query, and the README documents creating a least-privilege application user scoped to readWrite on the AAC database only. Second, I added indexing with query-plan evidence. An ensure_indexes() method creates single-field indexes on animal_type and breed and a compound index on outcome_type + datetime, chosen to match the dashboard's actual filter and aggregation patterns. The method is idempotent, and an explain_query() helper exposes the query planner output so the improvement can be shown as a move from a COLLSCAN (full collection scan) to an IXSCAN (index scan) on an indexed field. Choosing the compound index in outcome_type-then-datetime order is a deliberate design decision: it follows the index-prefix rule so the same index serves both an equality match on outcome type and a range scan over the date, which is precisely the shape of the new aggregation query. Third, I added an aggregation pipeline and used it to power a new dashboard view. An aggregate() method runs validated pipelines, and outcomes_by_type(start, end) implements a concrete "outcomes by animal type over a date range" pipeline ($match → $group → $project → $sort). The dashboard now renders that result as a stacked bar chart of outcome counts by animal type alongside the original pie chart, so the enhancement adds analytical value the user can see, not just internal machinery. Fourth, I added input validation and sanitization and a test suite. Every query now passes through a recursive scan that rejects dangerous MongoDB operators $where, $expr, $function, $accumulator, and the write-out stages $out/$merge even when they are nested inside a sub-document, which closes the classic NoSQL operator-injection vector where an attacker smuggles server-side JavaScript through a query filter. The delete() method refuses an empty filter so a bug can never wipe the whole collection. I then wrote an eighteen-test pytest suite that exercises CRUD, the injection guard, index creation, aggregation, and pagination against a dedicated in-memory test collection, so the data layer can be verified without touching a production database. Fifth, I implemented the pagination stretch goal. A read_page() method returns a bounded page of results plus metadata (page, page size, total, total pages), with the page size clamped to a maximum so a caller cannot request an unbounded result set and time out on the large AAC collection. A deliberate engineering decision runs through all of this, mirroring the seam I created in my algorithms enhancement: the AnimalShelter class accepts an injected client. In production it constructs a real PyMongo client from the configuration, but a test or the notebook can pass an in-memory mongomock client instead, with no other code path difference. That single seam is what lets the eighteen tests run anywhere and, importantly, lets the enhanced Jupyter notebook run end to end even on a machine with no access to the original Apporto MongoDB server: the notebook tries the configured database first and, if it is unreachable, transparently falls back to a mongomock database seeded from the CSV (or a small synthetic sample if the CSV is absent). I verified this by executing the entire notebook headlessly and confirming every cell runs without error in both the seeded-CSV and offline-fallback paths.

### Alignment with Course Outcomes

This enhancement meets the course outcomes I identified for the databases category in my Module One plan, and my outcome-coverage plan did not change.

- Outcome Three (design and evaluate computing solutions, managing trade-offs) the index design is an evaluated database decision: I chose which fields to index and the column order of the compound index to match the query shapes, and explain() provides the evidence for the scan-versus-index trade-off. Pagination is a further trade-off between round-trips and payload size, resolved by bounding the page.

- Outcome Four (well-founded and innovative techniques, skills, and tools)  I applied standard, well-founded database techniques: secondary and compound indexes, an aggregation pipeline, server-side pagination, dependency injection for testability, and an automated test suite, implemented with the appropriate tools (PyMongo, MongoDB aggregation, mongomock, pytest).

- Outcome Five (security mindset) this enhancement is one of the two primary carriers of the security outcome in my portfolio. It removes hardcoded credentials from source, connects through a least-privilege user, and sanitizes untrusted query input against operator injection, anticipating how a filter dictionary could otherwise be abused to run server-side code.

- Outcome Two (professional-quality communication) the new aggregation chart communicates a data result visually, and the README, inline documentation, and this narrative communicate the design and its rationale to a technical reader.

As with my other artifacts, Outcome One (collaboration and communicating to diverse audiences) is the outcome this piece does not fully carry on its own; I am covering it across the portfolio through the Module Two code review, the professional self-assessment, and the professional repository artifacts.

### Reflection on the Process

The most valuable lesson from this enhancement was how much database skill is really about anticipating the environment the code will run in rather than writing clever queries. The original artifact was not wrong, it worked in the classroom but every design shortcut it took (hardcoded connection, unbounded reads, no indexes, raw query passthrough) was an assumption that the world outside that classroom would look identical. Reworking it forced me to make each of those assumptions explicit and then remove it: configuration instead of hardcoding, bounded pages instead of unbounded reads, indexes instead of implicit full scans, and a validated query contract instead of blind passthrough. That is the same discipline I use professionally when a pricing configuration that behaves fine at one station has to be trusted across thousands of them. The main challenge was verification, and it turned into the most instructive part of the work. The original artifact was written against a specific hosted MongoDB instance I no longer had access to, and the machine I was working on could not reach it. My first instinct was to treat that as a blocker. Instead, I used it to force a better design: because I made the data layer accept an injected client, I could seed an in-memory mongomock database from the same CSV the project ships with and exercise the entire stack CRUD, indexing, aggregation, pagination, and the sanitization guard with no live server at all. I had to be honest about the one limit of that approach: mongomock does not implement explain(), so the query-plan evidence only appears against a real MongoDB. Rather than fake it, I made explain_query() degrade cleanly and print a clear "not supported offline" message, and I documented that the COLLSCAN-to-IXSCAN evidence should be captured against the live database. Being precise about what I could prove locally (that every enhancement runs correctly) versus what requires the production engine (the literal query plan) is itself part of evaluating a database solution honestly. I also learned to respect how the shape of the data constrains the query design. The AAC datetime field is stored as a string, so my date-range aggregation compares ISO date strings rather than native date objects; that works because the timestamps are zero-padded and lexicographically ordered, but recognizing why it works and that it would break for a differently formatted date is the kind of detail that separates a query that happens to return the right answer from one I actually understand. What I am most confident about now is that I can take a naive data component and reason about it along every axis that matters in practice security, performance, correctness, and testability and then prove the result runs, which is exactly the database competency this portfolio needs to show.
