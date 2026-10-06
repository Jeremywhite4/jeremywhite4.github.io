---
layout: default
title: Software Design & Engineering - WeightTracker
---

# Software Design & Engineering

**Enhancement One of Three**

### Artifact: WeightTracker (CS-360 - Java / Android / SQLite)

| | |
|---|---|
| **Original course** | CS-360 Mobile Architecture and Programming (June 2026) |
| **Language / stack** | Java, Android SDK, SQLite |
| **Category** | Software Design and Engineering (Milestone Two) |

**Code:**
[Original artifact](artifacts/software-engineering/original/) &nbsp;|&nbsp;
[Enhanced artifact](artifacts/software-engineering/enhanced/) &nbsp;|&nbsp;
[Enhancement narrative (Word)](narratives/CS499_Enhancement1_Narrative_Jeremy_White.docx)

[&larr; Back to portfolio home](index.md)

---

### What changed

| Area | Original | Enhanced |
|------|----------|----------|
| Architecture | Activities call `DatabaseHelper` directly | Layered **MVP + Repository**; `UserStore` interface (Dependency Inversion) |
| Password storage | **Plaintext** in the users table | **Salted PBKDF2-HMAC-SHA256**, constant-time verification |
| Login failures | Distinct errors reveal which field was wrong | Generic message prevents **user enumeration** |
| Input handling | `Double.parseDouble()` on raw input (crash risk) | Centralized `InputValidator` |
| Testing | None | JUnit (validator, auth) + Robolectric (hasher) |

---

### Description of the Artifact and Justification for Its Inclusion

The artifact I selected for the Software Design and Engineering category is WeightTracker, an Android application I originally built as the final project for CS-360: Mobile Architecture and Programming in June 2026. It is written in Java against the Android SDK and uses a local SQLite database. The application allows a user to create an account and log in, record daily weight entries, set a goal weight, and view a scrollable history of past entries; when a logged weight meets the goal, the app can send an SMS notification. In its original form the data layer was centralized in a single `DatabaseHelper` class, and separate Activities handled login, adding weights, setting a goal, and viewing history.

I chose this artifact because it starts from a reasonable foundation but contains clear, authentic opportunities to demonstrate professional software-engineering skill without changing what the app does for the user. Specifically, the original code exhibited three problems that are common in early student work and that map directly to the skills this category is meant to showcase. First, the Activities talked to `DatabaseHelper` directly, coupling the user interface to the persistence layer. Second, and most seriously, the application stored passwords in plaintext: `addUser()` wrote the raw password to the users table, and `checkUser()` authenticated with a raw `SELECT ... WHERE password=?` comparison. Third, it had no automated tests and only minimal input handling, calling `Double.parseDouble()` directly on user input in a way that would throw an exception and crash the app on non-numeric text. These are precisely the kinds of structural, security, and quality issues that a software engineer is expected to identify and correct, which made WeightTracker an ideal vehicle for demonstrating the enhancement.

The components that best show my skills are the authentication path (which I re-engineered end to end), the introduction of a layered architecture around the existing Activities, and the new validation and test code that surround both.

### How I Improved the Artifact

I performed the planned enhancement in four coordinated areas, and I marked every change in the source with an `ENHANCEMENT 1` comment and a documenting file header so a reviewer or another developer can quickly see the intent behind each file.

First, I refactored the application into a layered architecture. The user interface now flows through distinct responsibilities: an Activity acts as a thin view, a `LoginPresenter` coordinates the login screen using a Model-View-Presenter pattern, an `AuthService` and a `WeightRepository` hold the business and data-access logic, and `DatabaseHelper` is now a pure data-access object. I also introduced a `UserStore` interface that the authentication service depends on rather than depending on the concrete database class. This application of the Dependency Inversion principle decouples the security logic from Android and SQLite and, importantly, makes it testable off-device.

Second, I replaced the plaintext credential handling with a secure implementation. I created a `PasswordHasher` utility that applies salted PBKDF2-with-HMAC-SHA256 hashing with a unique random salt for every user and verifies passwords using a constant-time comparison to avoid timing side channels. I changed the database schema (version two) so the users table stores a `password_hash` and `salt` instead of a plaintext password, and I removed the plaintext `checkUser` query entirely. In the authentication service I made login return an identical, generic failure message whether the username is unknown or the password is wrong, which prevents user enumeration.

Third, I added centralized input validation in a new `InputValidator` class that checks username format and length, enforces a reasonable password strength rule, and safely validates weight input before it is parsed, eliminating the crash risk in the original Activities. Fourth, I added automated tests: pure-JVM JUnit tests for the validator and the authentication service (using an in-memory fake of the `UserStore` interface, including a test that specifically proves the anti-user-enumeration behavior) and a Robolectric test for the password hasher that confirms passwords are never stored as plaintext, that salts are unique, and that verification accepts correct passwords and rejects incorrect ones.

### Alignment with Course Outcomes

This enhancement meets the course outcomes I identified in my Module One plan, and my coverage plan for this category did not change.

Outcome Two (professional communication): The enhanced code is consistently documented with intent-bearing file headers and clear inline comments, and it is accompanied by this narrative and a README that explain the design decisions to a technical audience. Outcome Three (design and evaluate computing solutions, managing trade-offs): The move to a layered, MVP-plus-Repository design is a deliberate architectural decision, and the PBKDF2 iteration count (120,000) is an explicit, documented trade-off that balances resistance to brute-force attacks against the CPU cost of running on a mobile device. Outcome Four (well-founded and innovative techniques and tools): I applied established techniques and tools - the Repository and MVP patterns, a standard hashing algorithm, Dependency Inversion via an interface, and JUnit/Robolectric test automation. Outcome Five (security mindset): The core of the enhancement is a security uplift: eliminating plaintext credential storage, adding salted hashing and constant-time comparison, preventing user enumeration, using parameterized queries, and treating all user input as untrusted through centralized validation.

The one outcome this enhancement does not fully carry on its own is Outcome One (collaboration and communicating to diverse audiences), which I am covering across the portfolio through the code review, the professional self-assessment, and professional repository artifacts such as the README and commit history.

### Reflection on the Process

The most valuable lesson from this enhancement was how much of good software engineering is about creating the right seams. The single change that unlocked everything else was extracting the `UserStore` interface: once the authentication logic depended on an abstraction instead of the concrete Android database class, I could move security-critical code out of the UI and write fast, reliable unit tests for it without an emulator. That reinforced, in a concrete way, why decoupling and dependency inversion matter beyond being textbook principles.

The main challenge was working within Android's constraints while keeping the logic testable. The password hasher depends on `android.util.Base64`, so its tests must run under Robolectric, whereas the validator and authentication tests run as plain JVM tests. Deciding where to draw that line, keeping the pure logic framework-independent and isolating the Android dependency, was a design judgment I had to make deliberately. I also had to make a pragmatic decision about the schema migration: because the old plaintext passwords cannot be converted into secure hashes without the original passwords, I documented and implemented a clean rebuild of the users table on upgrade rather than pretending a lossless migration was possible.

What I am most confident about now is my ability to take working-but-flawed code and raise it to a professional standard methodically: identify the structural and security defects, introduce layers and abstractions that make the system both safer and more testable, and prove the improvements with automated tests rather than simply asserting them. That is the same discipline I apply in my professional work on pricing systems, and this enhancement is strong evidence of it for my ePortfolio.
