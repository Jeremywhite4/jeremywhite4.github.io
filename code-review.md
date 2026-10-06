---
layout: default
title: Informal Code Review
---

# Informal Code Review

[&larr; Back to portfolio home](index.md)

Before enhancing any code, I produced an informal code review that walks through the
original state of all three artifacts and the enhancements I planned for each. For every
artifact the review covers, in order: **(1) existing functionality, (2) a code analysis
against the CS-499 checklist, and (3) the planned enhancement**, with the specific skills
and course outcomes each enhancement targets.

## Watch the video

> **Video link:** _The recorded code review is hosted on YouTube (unlisted) because the
> file is far larger than GitHub's size limit. Paste the YouTube URL here once uploaded:_
>
> **[▶ CS-499 Code Review — YouTube](REPLACE_WITH_YOUTUBE_URL)**

<!-- To embed the player instead of a plain link, replace VIDEO_ID below and uncomment:
<p>
<iframe width="720" height="405" src="https://www.youtube.com/embed/VIDEO_ID"
title="CS-499 Code Review" frameborder="0" allowfullscreen></iframe>
</p>
-->

## Code review script / narration outline

The full narration outline that structures the video is included for reference:

- [Code review script (Markdown)](code-review-script.md)

## What the review covers

| Artifact | Primary weakness identified | Planned enhancement |
|----------|-----------------------------|---------------------|
| [WeightTracker](software-engineering.md) | Plaintext password storage; UI coupled to the database | Salted PBKDF2 hashing + layered MVP/Repository architecture |
| [Pirate Intelligent Agent](algorithms.md) | O(n) `del memory[0]` eviction on a list buffer | deque ring buffer (O(1)) + prioritized replay (O(log n)) |
| [Animal Shelter Dashboard](databases.md) | Hardcoded credentials; no indexes; unvalidated queries | Credentials out of source, indexes with `explain()`, aggregation, sanitization |
