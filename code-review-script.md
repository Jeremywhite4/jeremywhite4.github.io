---
layout: default
title: Code Review Script
---

[&larr; Back to code review](code-review.md)

# CS 499 Code Review — Script / Narration Outline

Jeremy White

> Draft script for the recorded code review. Incorporates both the Code Review Checklist
> (George, n.d.) and the Milestone Two Code Review Overview (Brooke, n.d.).
>
> **KEY REQUIREMENTS FROM THE OVERVIEW VIDEO:**
> - **Target ~30 minutes.** Under ~15 min does NOT cover the required content. Aim 25-30+.
> - Open with a **Table of Contents** slide mirroring the rubric order.
> - For EACH artifact cover, in order: **(1) Existing Functionality → (2) Code Analysis
>   (walk the whole checklist) → (3) Enhancements (skills + course outcomes)**.
> - **Separate cleanup from enhancement.** Fixing errors, correcting/removing comments,
>   consistent formatting, and a complete header are final-portfolio cleanup, NOT the
>   enhancement. Say so explicitly, then describe the approved enhancement separately.
> - **Walk the ENTIRE checklist per artifact.** When a category does not apply (e.g., no
>   arithmetic operations, no manual memory management), SAY it does not apply and why -
>   don't skip silently.
> - Keep an **outcomes file visible on screen**; after describing an enhancement, say
>   "here is what I did, therefore these skills, therefore these course outcomes."
> - Informal tone is fine; small verbal slips are OK ("excuse me, correction..."). Optional
>   picture-in-picture webcam. Host externally (file too big for GitHub) and link back.
> - `[FEEDBACK]` = revisit after instructor returns enhancement-plan feedback.

---

## Screen Cue Sheet (bookmark these before recording)

Open files in this order per segment. Bookmark each cited line in your IDE so you can jump instantly.

| # | Segment | File to open | Headline (PRIMARY WEAKNESS) line |
|---|---------|--------------|----------------------------------|
| 1 | SW Eng — WeightTracker | `DatabaseHelper.java` (172 ln), `LoginActivity.java` (91 ln) | plaintext store `DatabaseHelper.java` **L72**; `checkUser` `SELECT *` **L83–85** |
| 2 | Algorithms — Pirate Agent | `GameExperience.py` (83 ln), `Jeremy_White_ProjectTwo.ipynb` | O(n) eviction `GameExperience.py` **L27** (`del self.memory[0]`) |
| 3 | Databases — Animal Shelter | `animal_shelter.py` (48 ln), `ProjectTwoDashboard.ipynb` | hardcoded creds `animal_shelter.py` **L10–13, L16** |

Second tab throughout: the five-course-outcomes reference file.

---

## 0. Table of Contents slide + Intro (about 2 min)

**On screen: a simple TOC slide.**
```
CS 499 Code Review — Jeremy White — [DATE]
  Artifact 1 (Category 1 - Software Engineering & Design): WeightTracker
  Artifact 2 (Category 2 - Algorithms & Data Structures): Pirate Intelligent Agent
  Artifact 3 (Category 3 - Databases): Animal Shelter Dashboard
  For each: Existing Functionality -> Code Analysis (checklist) -> Enhancements (skills + outcomes)
```
Also have an **Outcomes reference file open** in a second tab (the five course outcomes).

Narration: "Hi, I'm Jeremy White; this is my CS 499 code review, recorded [date]. I'm using
three artifacts, one per category. For each I'll cover the existing functionality, analyze the
code against the CS 499 checklist, and then describe the approved enhancement, the skills it
demonstrates, and the course outcomes it meets. One note up front: some of what I point out -
correcting comments, formatting, headers - is cleanup I'll do for the final portfolio and is
separate from the actual enhancement; I'll be clear about which is which. Code reviews improve
both the code and the developer, so I'll be candid about the weaknesses in my own earlier work."

---

## Segment 1 — Software Engineering & Design: WeightTracker (Java / Android / SQLite) (~8 min)

### 1a. Existing functionality (about 1.5 min)
- CS-360 final project (June 2026). Android/Java over local SQLite. Users register/log in, log
  daily weights, set a goal weight, view history.
- Data layer centralized in `DatabaseHelper`; Activities for login, add-weight, goal, history.

> **SHOW ON SCREEN:**
> - `DatabaseHelper.java` — full file (172 lines). Scroll through the two tables + CRUD.
> - `LoginActivity.java` — full file (91 lines). Show the login/create-account flow.
>
> | To point at | File | Lines |
> |---|---|---|
> | Two tables defined (users + weights constants) | `DatabaseHelper.java` | L18–29 |
> | Table creation SQL | `DatabaseHelper.java` | L37–54 |
> | Activity wiring (login + create-account handlers) | `LoginActivity.java` | L24–37 |

### 1b. Code analysis — FULL checklist walk (about 4 min)
Go category by category; name each, even when it's a strength or N/A.
- **Structure:** Implements the design; genuine separation of concerns (all SQL in
  `DatabaseHelper`, Activities call it). No unreachable code or leftover stubs. No large
  duplicated blocks. Modules aren't excessively complex. *Strength.*
  - *Show:* CRUD grouped into USER vs WEIGHT sections — `DatabaseHelper.java` L64 (users banner)
    and L118 (weights banner).
- **Documentation:** Every method has a Javadoc-style comment; comments are consistent with the
  code. *Strength* (note: for final-portfolio cleanup I'll add a complete file header with
  intent/decision - that's cleanup, not the enhancement).
  - *Show:* Javadoc block above each method, e.g. `DatabaseHelper.java` L66–69, L76–79; class
    comment L9–12. Contrast with the missing intent/decision header at top of file (L1).
- **Variables:** Table/column names are symbolic constants, not magic strings; names are clear
  and consistent; types are appropriate. *Strength.*
  - *Show:* Symbolic constants — `DatabaseHelper.java` L18–29.
- **Arithmetic operations:** **Not applicable** - no floating-point comparisons, division, or
  rounding logic in this data layer. Say so and move on.
- **Loops and branches:** Minimal branching; the empty-field checks are simple and correct.
  - *Show:* Empty-field guards — `LoginActivity.java` L49–52 (login) and L76–79 (create account).
- **Defensive programming / Security (PRIMARY WEAKNESS):**
  - `addUser()` stores the password exactly as typed; `checkUser()` authenticates with
    `SELECT * FROM users WHERE username=? AND password=?` - so credentials are stored in
    **plaintext**. Headline finding.
    - *Show:* `addUser()` plaintext store — `DatabaseHelper.java` L67–74 (the `values.put(COL_PASSWORD, password)` at L72).
    - *Show:* `checkUser()` query — `DatabaseHelper.java` L80–88 (the `SELECT *` at L83–85).
  - Positive to note: the `?` placeholders mean it is **not** SQL-injectable - good.
    - *Show:* parameterized `new String[]{username, password}` — `DatabaseHelper.java` L85.
  - Input validation is limited to empty-field checks (no strength/format validation).
    - *Show:* `LoginActivity.java` L49–52.
  - Same-message-for-both positive to note: `LoginActivity.java` L64 already returns one
    "Invalid username or password" (no user enumeration) — call this out as an existing strength.
  - No automated unit or instrumented tests.

### 1c. Enhancements — skills + outcomes (about 2 min)
Cleanup (final portfolio, NOT the enhancement): add file header, tidy comments, consistent format.
Approved enhancement:
1. Replace plaintext with salted PBKDF2 hashing + constant-time compare; identical failure
   message for bad username vs. bad password (prevent user enumeration).
2. Add a Repository + presenter/ViewModel layer so Activities no longer touch persistence.
3. Add structured input validation and error handling.
4. Add unit tests (repository/auth) + instrumented tests (DAO).
"Here is what I did, therefore these skills: secure credential handling, layered architecture/
refactoring, defensive programming, test automation. Therefore these outcomes: **Outcome 5**
(security, primary), **Outcome 3/4** (design + sound tools), **Outcome 2** (documented code)."
`[FEEDBACK: confirm PBKDF2 vs bcrypt acceptable.]`

---

## Segment 2 — Algorithms & Data Structures: Pirate Intelligent Agent (Python / TensorFlow) (~8 min)

### 2a. Existing functionality (about 1.5 min)
- CS-370 Project Two (Aug 2026). Deep Q-learning agent navigates an 8x8 maze to the treasure.
  Environment in `TreasureMaze.py`; experience-replay memory in `GameExperience.py`.

> **SHOW ON SCREEN:**
> - `GameExperience.py` — full file (83 lines). This is the replay-memory class.
> - `Jeremy_White_ProjectTwo.ipynb` — scroll to the `qtrain()` loop cell and the `play_game()` cell.
>
> | To point at | File | Lines |
> |---|---|---|
> | Class + constructor (memory as a `list`) | `GameExperience.py` | L6–19 (list init at L17) |
> | `remember()` | `GameExperience.py` | L21–27 |
> | `sample()` / `get_data()` | `GameExperience.py` | L36–43 / L45–75 |
> | `qtrain()` training loop | `Jeremy_White_ProjectTwo.ipynb` | (bookmark the qtrain cell) |

### 2b. Code analysis — FULL checklist walk (about 4 min)
- **Structure:** Clean separation (environment / replay / training loop); small, single-purpose
  methods; already uses a target network and batched prediction in `get_data()`. *Strength.*
  - *Show:* batched prediction — `GameExperience.py` L64–65 (`self.model(states...)` / `q_next`).
- **Documentation:** Classes/methods commented; training loop prints per-epoch metrics.
  - *Show:* class/param comments — `GameExperience.py` L1–2, L8–11; docstrings L37–39, L46–49.
- **Variables:** Descriptive names (`epsilon`, `win_history`, `max_memory`); the action
  constants LEFT/UP/RIGHT/DOWN are symbolic, not magic numbers. *Strength.*
  - *Show:* `max_memory` / `discount` params — `GameExperience.py` L13–16; action constants live
    in `TreasureMaze.py` (bookmark the LEFT/UP/RIGHT/DOWN block).
- **Arithmetic operations:** Present but sound - the reward math and Q-update use floats without
  relying on float equality; no divide-by-zero risk (denominators are fixed sizes). State this.
  - *Show:* Q-update math — `GameExperience.py` L70–74 (`reward + self.discount * np.max(...)`).
- **Loops and branches:** Loops terminate (game-over/step cap). **Weakness:** the exploit branch
  calls `model.predict(...)` *inside* the inner loop - high per-call overhead vs. a direct
  `model(x)` call (the notebook already uses the faster form in `play_game()`, so it's an
  inconsistency; checklist: "can statements inside loops be moved / is this efficient").
  - *Show:* the slow call — `Jeremy_White_ProjectTwo.ipynb`, exploitation `else` branch of the
    `while not game_over` loop in `qtrain()`:
    `q_values = model.predict(state_reshaped, verbose=0)`.
  - *Then contrast with the fast form* used elsewhere in this same project:
    `GameExperience.py` L30–34 (`predict()` calls `self.model(envstate, training=False)`), and
    `play_game()` in the notebook (`q_values = model(state, training=False).numpy()`).
  - *Say this:* Both compute the same Q-values; the difference is overhead. `model.predict()` is
    Keras's **batch-inference API** - built for throughput on large datasets, so it re-sets up a
    prediction/batching path and callback bookkeeping on every call. That's fine once on 10,000
    rows; here it runs **once per step on a single state**, thousands of times, so the setup cost
    dwarfs the actual math. `model(x, training=False)` is a **direct forward pass** - same result,
    no batching machinery - which is the right tool for single-sample, high-frequency calls.
  - *The strongest framing is the inconsistency:* I don't have to argue this from theory - the
    hot path (`qtrain` inner loop) uses the slow API while the cold paths (`play_game`, my own
    `GameExperience.predict`) already use the fast one. That's backwards.
  - *One-line fix (this is CLEANUP, not the enhancement):*
    `q_values = model(state_reshaped, training=False).numpy()`. My headline Segment 2 enhancement
    is still the O(n)→O(1) replay-buffer fix at `GameExperience.py` L27; this is a supporting
    efficiency observation I noticed during the checklist walk.
- **Defensive programming:** No fixed random seed -> runs aren't reproducible, so improvements
  can't be measured cleanly. Manual memory management is **N/A** (Python GC) - say so.
- **Efficiency / storage (PRIMARY WEAKNESS):** In `remember()`, when memory exceeds capacity the
  code does `del self.memory[0]` on a Python `list` - deleting from the front is **O(n)** because
  every element shifts. Repeated thousands of times. Show the exact line.
  - *Show:* **`GameExperience.py` L27 — `del self.memory[0]`** (the headline line). Show the
    capacity check at L26 for context.

### 2c. Enhancements — skills + outcomes (about 2 min)
Cleanup (final portfolio): consistent formatting, header with intent/decision.
Approved enhancement:
1. Replace the `list` replay buffer with `collections.deque(maxlen=N)` -> **O(1)** eviction,
   manual capacity check removed. Show before/after complexity table.
2. Add an epsilon-decay exploration schedule; quantify convergence.
3. Add a seeded benchmark harness: episodes-to-stable-win-rate, avg steps-to-goal, wall-clock
   time, replay memory footprint. (Stretch: prioritized replay via sum-tree, O(log n) sampling.)
"Skills: data-structure selection justified by complexity analysis, algorithm tuning, empirical
evaluation with reproducible benchmarks. Outcomes: **Outcome 3** (algorithmic design +
accuracy/performance trade-off, primary), **Outcome 4** (RL/benchmarking tools), **Outcome 2**
(evaluation via tables/charts)."
`[FEEDBACK: confirm data-structure + convergence work qualifies as the algorithms enhancement
vs. adding a brand-new algorithm - open question in the Module 1 plan.]`

---

## Segment 3 — Databases: Animal Shelter Dashboard (Python / PyMongo / MongoDB) (~8 min)

### 3a. Existing functionality (about 1.5 min)
- CS-340 Project Two (June 2026). `AnimalShelter` CRUD module over a MongoDB collection of
  Austin Animal Center data, plus a Dash/Plotly dashboard that filters and visualizes it.

> **SHOW ON SCREEN:**
> - `animal_shelter.py` — full file (48 lines). All four CRUD methods fit on one screen.
> - `ProjectTwoDashboard.ipynb` — scroll to the callback that calls `read()` and the chart cells.
>
> | To point at | File | Lines |
> |---|---|---|
> | Class + constructor | `animal_shelter.py` | L5–18 |
> | `create()` / `read()` / `update()` / `delete()` | `animal_shelter.py` | L20–25 / L27–32 / L34–39 / L41–47 |

### 3b. Code analysis — FULL checklist walk (about 4 min)
- **Structure:** CRUD cleanly separated into create/read/update/delete; dashboard decoupled from
  the data layer. No dead code. *Strength.*
  - *Show:* the four methods — `animal_shelter.py` L20–47.
- **Documentation:** Class docstring present; methods short and readable. (Cleanup: expand
  method-level comments for the final portfolio - not the enhancement.)
  - *Show:* class docstring — `animal_shelter.py` L6; note the methods L20–47 have no docstrings.
- **Variables:** `USER/PASS/HOST/PORT/DB/COL` are named, but the connection values are
  **hardcoded literals** in `__init__` - flag under Security below.
  - *Show:* `animal_shelter.py` L10–13.
- **Arithmetic operations:** **Not applicable** - no numeric computation in the CRUD layer.
- **Loops and branches:** Each method uses a simple `if input is not None` guard and raises a
  clear exception otherwise. *Reasonable defensive baseline.*
  - *Show:* guard + raise pattern — `animal_shelter.py` L21/L24 (create), mirrored in each method.
- **Defensive programming / Security (PRIMARY WEAKNESS):**
  - `__init__` **hardcodes** `HOST`, `PORT`, `DB`, `COL` in source and injects credentials into
    the connection string; nothing from environment/config; no least-privilege user. Headline.
    - *Show:* hardcoded literals — `animal_shelter.py` L10–13; connection string with injected
      creds at L16.
  - `read()` passes the caller's `query` dict straight into `find()` with no validation - unusual
    operators aren't rejected.
    - *Show:* `animal_shelter.py` L27–32 (the raw `self.collection.find(query, ...)` at L29).
  - Files/connections: N/A for file handles, but the Mongo client is never explicitly closed -
    note for cleanup.
    - *Show:* client created at L16, no corresponding `close()` anywhere in the file.
- **Efficiency / storage:** No indexes -> filtered `read()` triggers full **collection scans**.
    - *Show:* `read()` — `animal_shelter.py` L27–32 (no index referenced/created anywhere).
- **Structure (gaps):** No aggregation pipeline; no automated tests.

### 3c. Enhancements — skills + outcomes (about 2 min)
Cleanup (final portfolio): method comments, header, close the client cleanly.
Approved enhancement:
1. Move connection/credentials to environment/config; add connection error handling; connect
   with a least-privilege DB user.
2. Create indexes (animal_type, breed, compound outcome_type+datetime); show `explain()`
   before/after proving index usage.
3. Add an aggregation pipeline (outcomes by type over a date range) powering a new dashboard view.
4. Validate/sanitize query filters; add unit tests against a dedicated test collection.
"Skills: secure/least-privilege data access, index design + query-plan analysis, aggregation,
input sanitization, testing a data layer. Outcomes: **Outcome 5** (secure data, primary),
**Outcome 3** (index/aggregation design with measured trade-offs), plus **Outcome 4** and
**Outcome 2**."

---

## 4. Close (about 1 min)

"To summarize: WeightTracker centers on secure credential handling and a layered architecture;
the Pirate agent on an O(n)-to-O(1) replay-buffer fix with measured convergence; and the Animal
Shelter module on secure configuration, indexing, and aggregation. Everything I flagged as
formatting, comments, or headers is final-portfolio cleanup; the enhancements themselves are the
security, algorithm, and database work I just described. Together they cover all five course
outcomes. Thanks for watching - I welcome feedback and will incorporate it before I start the
enhancements."

---

## Pre-record checklist
- [ ] **Length ~30 min** (25-30+). A full checklist walk per artifact gets you there; don't rush.
- [ ] Instructor feedback on enhancement plan incorporated (`[FEEDBACK]` tags resolved)
- [ ] TOC slide ready; five-outcomes file open in a second tab
- [ ] IDE font size bumped; only relevant files open; each cited line bookmarked (see Screen Cue Sheet + the per-segment "SHOW ON SCREEN" tables)
- [ ] Decide on optional picture-in-picture webcam
- [ ] Mic test; export MP4; host externally (too big for GitHub); link from ePortfolio
- [ ] Remember: submitted twice (Module 2 for grade/feedback; again with final portfolio)

## References (for the accompanying narrative/journal, not spoken)
- George, C. (n.d.). *CS 499 transcript for checklist for code reviews* [Transcript]. SNHU.
- Brooke. (n.d.). *CS 499 Milestone Two code review overview / approach assistance* [Transcript]. SNHU.

