---
layout: default
title: Informal Code Review
---

# Informal Code Review

[&larr; Back to portfolio home](index.md)

This is my single informal code review for the capstone, recorded once in Module Two
(Milestone One) and covering all three artifacts in one video. Before enhancing any code, I
walk through the original state of each artifact and the enhancements I planned for it. For
every artifact the review covers, in order: **(1) existing functionality, (2) a code analysis
against the CS-499 checklist, and (3) the planned enhancement**, with the specific skills and
course outcomes each enhancement targets.

## Watch the video

The recorded code review (`CS-499 Module 2 Code Review.mp4`) is hosted on YouTube as an
unlisted video, because the file (about 870 MB) is far larger than GitHub's size limit.

**[▶ Watch the CS-499 Code Review on YouTube](https://youtu.be/vbVR2bgEGLk)**

<p>
<iframe width="720" height="405"
src="https://www.youtube-nocookie.com/embed/vbVR2bgEGLk?rel=0"
title="CS-499 Code Review" frameborder="0"
allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
referrerpolicy="strict-origin-when-cross-origin"
allowfullscreen></iframe>
</p>

_If the player above shows "video unavailable," do a hard refresh (Ctrl+F5) to clear a cached
copy, or use the direct link above._

## What the review covers

| Artifact | Primary weakness identified | Planned enhancement |
|----------|-----------------------------|---------------------|
| [WeightTracker](software-engineering.md) | Plaintext password storage; UI coupled to the database | Salted PBKDF2 hashing + layered MVP/Repository architecture |
| [Pirate Intelligent Agent](algorithms.md) | O(n) `del memory[0]` eviction on a list buffer | deque ring buffer (O(1)) + prioritized replay (O(log n)) |
| [Animal Shelter Dashboard](databases.md) | Hardcoded credentials; no indexes; unvalidated queries | Credentials out of source, indexes with `explain()`, aggregation, sanitization |
