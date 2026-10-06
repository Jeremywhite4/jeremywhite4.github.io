---
layout: default
title: Professional Self-Assessment
---

# Professional Self-Assessment

[&larr; Back to portfolio home](index.md)

## Introduction

I am a software and data professional completing a Bachelor of Science in Computer Science
at Southern New Hampshire University, and this ePortfolio is the capstone of roughly three
years of study. Over that time I moved from foundational programming through data structures,
software testing, mobile development, databases, and secure coding, and I was fortunate to
apply much of it in parallel in my day job, where I work as a program manager on Amazon Flex
pricing alongside machine-learning models, large datasets, and configuration systems. The
program did not just teach me new tools; it gave me a vocabulary and a discipline for the
engineering instincts I was already reaching for at work, and it pushed me toward a clear
specialization: **applied machine learning and data-driven backend engineering.**

Building this portfolio clarified both my strengths and my goals. My strengths are taking a
system that works but is naive, reasoning about it along every axis that matters in practice
(correctness, performance, security, and testability), and then proving the result rather than
asserting it. My goal is to keep moving deeper into ML- and data-centric systems engineering,
and the three enhancements here were chosen deliberately to demonstrate exactly that trajectory.
What follows introduces the skills I bring, with specific examples from across the program and
from work beyond the three artifacts, and then explains how those artifacts fit together as a
single body of evidence.

## Collaborating in a Team Environment

Although my three artifacts are individual projects, collaboration is central to how I work and
how I was trained in this program. In CS-250 (Software Development Lifecycle) I worked in an
Agile/Scrum framing, translating user stories into development tasks and reasoning about work
from the perspective of a whole delivery team rather than a lone coder. That experience maps
directly onto my professional life: on Amazon Flex pricing I operate at the seam between
science, engineering, and operations teams, where no change ships without aligning people who
hold different pieces of the system. The informal [code review](code-review.md) in this
portfolio is itself a collaboration artifact. Its audience is a peer or a manager, so I had to
present my own earlier work honestly, name its weaknesses, and justify a plan in terms a
reviewer could challenge, which is the everyday reality of building a shared understanding
before changing code. I also treat professional repository hygiene, clear READMEs, intent-bearing
commit messages, and documented design decisions, as a form of asynchronous collaboration: they
let a diverse audience of future contributors understand and support decisions without me in the
room.

## Communicating with Stakeholders

Communicating with stakeholders is the part of my professional work I lean on most, and the
program sharpened it. Every week I translate model behavior and cost trade-offs into language
that non-engineers, finance partners, operations leaders, and senior management, can act on. The
same skill runs through this portfolio. Each enhancement is accompanied by a written narrative
that explains not just what I did but why, pitched at a technical reader, and the
[databases](databases.md) enhancement adds a new aggregation-driven visualization precisely
because a chart communicates a data result to a decision-maker faster than a table of documents.
I deliberately separated cleanup from enhancement in the code review, and I was explicit in the
[algorithms](algorithms.md) narrative about what I could prove locally versus what requires the
production engine, because credible communication means being precise about the limits of your
evidence, not overselling it.

## Data Structures and Algorithms

The program trained me to choose the right data structure for a problem and to reason about time
and space complexity, and the [Pirate Intelligent Agent](algorithms.md) enhancement is my
clearest demonstration of it. The original agent stored its experience-replay memory in a plain
Python list and evicted the oldest item with `del memory[0]`, an O(n) operation that shifts
every remaining element and runs tens of thousands of times per training run. I replaced it with
a `collections.deque(maxlen=N)` ring buffer for O(1) eviction, and then went further and
implemented proportional prioritized experience replay on a sum tree, which makes drawing and
updating a weighted sample O(log n) rather than O(n), with importance-sampling weights to correct
the resulting bias. I proved the improvement empirically with a seeded benchmark: at a buffer
capacity of 16,000 the deque was roughly 83x faster than the list, and prioritized replay sampled
a rare, high-value transition about 46x more often than uniform sampling. That instinct, that a
quiet data-structure choice can silently dominate a system's performance, traces back to CS-260
and is the same instinct I use when a pricing configuration that behaves fine at one station has
to be trusted across thousands.

## Software Engineering and Databases

My software-engineering foundation is about creating the right seams. In the
[WeightTracker](software-engineering.md) enhancement I refactored an Android app whose Activities
talked directly to a `DatabaseHelper` into a layered design: a thin view, a `LoginPresenter`
(Model-View-Presenter), an `AuthService` and `WeightRepository` holding business and data logic,
and a `UserStore` interface so the security code depends on an abstraction instead of SQLite. That
single seam, extracting the interface, is what let me move credential logic out of the UI and unit
test it off-device. This builds on separation-of-concerns work I did across CS-340, CS-360, and
CS-465 (a MEAN-stack project). On the database side, the [Animal Shelter](databases.md) enhancement
took a minimal PyMongo CRUD wrapper with a hardcoded connection and raised it to a performant,
testable data layer: secondary and compound indexes with `explain()` evidence of a collection-scan
to index-scan improvement, an aggregation pipeline powering a new chart, bounded pagination, and an
18-test pytest suite that runs against an in-memory `mongomock` client so it needs no live server.
Deciding what I could verify offline versus what required the production engine was itself part of
evaluating the solution honestly.

## Security

A security mindset runs through everything I build, grounded in CS-320 (testing and quality) and
CS-405 (secure coding), where I learned to treat all input as untrusted and to recognize
vulnerabilities such as plaintext credential storage and unsafe query construction. The
[WeightTracker](software-engineering.md) enhancement is the sharpest example: the original app
stored passwords in plaintext and authenticated with a raw `SELECT`. I replaced that with salted
PBKDF2-HMAC-SHA256 hashing and constant-time verification, and I made login return an identical
generic failure whether the username or the password is wrong, closing a user-enumeration channel.
The [databases](databases.md) enhancement carries the same mindset to data: credentials moved out
of source and into configuration, a documented least-privilege database user, and sanitization that
rejects dangerous NoSQL operators like `$where` so a filter dictionary cannot be turned into
server-side code execution. These are the same instincts I apply professionally when deciding how
and where sensitive pricing data is stored and queried, and they are the capabilities I expect to
rely on even more as computing pushes toward distributed, device-heavy, and post-quantum-aware
environments.

## How the Artifacts Fit Together

The three artifacts are intentionally distinct projects, chosen so the portfolio shows range
rather than one idea stretched three ways, yet together they trace a single professional profile.
The [algorithms](algorithms.md) enhancement (a deep reinforcement-learning agent) demonstrates the
machine-learning and complexity depth at the center of my target specialization. The
[databases](databases.md) enhancement (indexing and aggregation over a NoSQL store) demonstrates the
data-engineering half of that same specialization. The [software-engineering](software-engineering.md)
enhancement (a secure, layered application) demonstrates that I can also deliver production-quality,
well-architected, secure software around that ML and data work. A common discipline links all three:
in each case I found a working-but-naive artifact, made its hidden assumptions explicit, replaced them
with well-founded techniques, and proved the result with tests, benchmarks, or query-plan evidence.
Read together with the [code review](code-review.md), they are designed to present me as an engineer
who can build, evaluate, and secure the ML- and data-centric systems I intend to work on. The
technical artifacts that follow this assessment provide the detailed evidence for each claim made here.

---

## Program Outcome Coverage

| Outcome | Where it is demonstrated |
|---------|--------------------------|
| **1: Collaborative environments / diverse audiences** | Code review (peer / manager audience), this self-assessment, Agile/Scrum work in CS-250, and professional repository artifacts (READMEs, commit history) |
| **2: Professional communication (oral/written/visual)** | All three narratives, READMEs, the benchmark charts, and the new dashboard visualization |
| **3: Design/evaluate solutions, manage trade-offs** | PBKDF2 iteration-count trade-off; list to deque and prioritized replay; index design with `explain()` and bounded pagination |
| **4: Well-founded & innovative techniques/tools** | MVP + Repository, PBKDF2, JUnit/Robolectric; target networks, PER, sum tree; indexes, aggregation, pytest/mongomock |
| **5: Security mindset** | Salted hashing + constant-time compare, anti-enumeration; least-privilege access, credentials out of source, NoSQL injection sanitization |
