---
layout: default
title: Algorithms & Data Structures - Pirate Intelligent Agent
---

# Algorithms & Data Structures

**Enhancement Two of Three**

### Artifact: Pirate Intelligent Agent (CS-370 - Python / deep Q-learning)

| | |
|---|---|
| **Original course** | CS-370 Current and Emerging Trends (August 2026) |
| **Language / stack** | Python, TensorFlow / Keras, NumPy |
| **Category** | Algorithms and Data Structures (Milestone Three) |

**Code:**
[Original artifact](artifacts/algorithms/original/) &nbsp;|&nbsp;
[Enhanced artifact](artifacts/algorithms/enhanced/) &nbsp;|&nbsp;
[Enhanced README (with benchmarks)](artifacts/algorithms/enhanced/README.md) &nbsp;|&nbsp;
[Enhancement narrative (Word)](narratives/CS499_Enhancement2_Narrative_Jeremy_White.docx)

[&larr; Back to portfolio home](index.md)

---

### What changed

| Area | Original | Enhanced |
|------|----------|----------|
| Replay buffer store | Python `list` | `collections.deque(maxlen=N)` ring buffer |
| Full-buffer eviction | `del memory[0]` - **O(n)** | auto-evict oldest - **O(1)** |
| Sampling | Uniform random | **Prioritized experience replay** via a sum tree - O(log n) |
| Bias correction | None | Importance-sampling weights |
| Exploration | Ad-hoc epsilon nudges | **Exponential epsilon-decay** schedule |
| Evaluation | Win rate only | Seeded **benchmark harness** (epochs-to-95%, steps-to-goal, wall-clock) |

Measured: at a buffer capacity of 16,000 the deque is roughly **83x** faster than the
list on full-buffer eviction, and prioritized replay sampled a rare high-value transition
about **46x** more often than uniform sampling.

---

# Algorithms and Data Structures - Pirate Intelligent Agent

### Description of the Artifact and Justification for Its Inclusion

The artifact I selected for the Algorithms and Data Structures category is the **Pirate Intelligent Agent**, a reinforcement-learning program I originally built as Project Two for CS-370: Current and Emerging Trends in Computer Science in August 2026. It is written in Python and uses TensorFlow/Keras. The agent is a "pirate" that learns to navigate an 8×8 maze to reach the treasure using deep Q-learning: a neural network approximates the Q-function (the expected future reward of taking each action from a given state), and the agent improves by replaying past experience. The original project is organized into three parts: TreasureMaze.py (the environment, which defines the maze, the legal moves, and the reward scheme), GameExperience.py (the experience-replay buffer that stores past transitions for the network to learn from), and a Jupyter notebook containing the qtrain() deep Q-learning training loop.

I chose this artifact because it is genuinely algorithmic and because it contained clear, measurable opportunities to demonstrate data-structure and algorithm-design skill without changing what the program does. The most important opportunity was in the experience-replay buffer. In the original code the buffer was a plain Python list, and when it reached capacity it evicted the oldest transition with del self.memory[0]. Deleting the first element of a list is an O(n) operation, because CPython must shift every remaining element one slot to the left. Because that eviction runs on every step of every training episode, tens of thousands of times per run, it is exactly the kind of hidden inefficiency that a computer scientist is expected to recognize and fix. The second opportunity was in *how* the agent learned from that buffer: it sampled past transitions uniformly at random. In a sparse-reward maze the overwhelming majority of stored transitions are unremarkable single steps, so uniform sampling spends almost all of its training budget on transitions the network has already mastered while rarely revisiting the few decisive transitions (reaching the treasure, hitting a wall) that actually carry the learning signal.

The components that best showcase my skills are therefore the replay buffer itself, which I re-engineered from a list into a bounded ring buffer with a prioritized-sampling data structure, and the training loop, where I replaced ad-hoc exploration control with a principled schedule and added a reproducible benchmark to prove the improvements empirically rather than merely asserting them.

### How I Improved the Artifact

I performed the planned enhancement in four coordinated areas, and, as in my first enhancement, I marked every change in the source with an ENHANCEMENT 2 comment and a documenting file header so a reviewer can immediately see the intent behind each file.

First, I replaced the list-based buffer with a ring buffer implemented as collections.deque(maxlen=N). A deque is a doubly linked list of fixed-size blocks, so appends and evictions at either end are O(1); setting maxlen makes a full buffer evict its oldest element automatically, with no length check and no element shifting. This drops the full-buffer insertion from O(n) to O(1). To make the analysis concrete I wrote a small benchmark (benchmark_replay.py) that times the two strategies as the capacity grows. The list's cost scales roughly linearly with capacity while the deque stays flat: at a capacity of 1,000 the deque is about 10× faster, and by a capacity of 16,000 it is about 83× faster (0.457 s versus 0.006 s). That widening gap is the visible signature of O(n) versus O(1) behavior.

Second, I implemented proportional prioritized experience replay (PER) to make each training sample more informative. Instead of sampling uniformly, the buffer now samples each transition with probability proportional to its most recent temporal-difference (TD) error, the transitions the network is most "surprised" by are replayed more often. I built this on a sum tree, a binary-heap-shaped array in which each internal node stores the sum of its children, so drawing a prioritized sample and updating a priority are both O(log n) rather than the O(n) a naive priority scan would cost. Because biased sampling would distort the expected gradient, I added importance-sampling weights that scale each sampled transition's contribution to the training loss, correcting the bias, and new transitions are inserted at maximum priority so every experience is trained on at least once. (Applying those weights to the loss is essential: prioritized sampling without the correction trains the network on a skewed distribution and can perform no better than, or worse than, uniform sampling. Under uniform replay every weight is 1.0, so the same code path reduces exactly to the original mean-squared-error loss.) My data-structure benchmark confirms the sampling behavior: in a buffer of 2,000 transitions containing one high-value transition, PER replayed that decisive transition about 46× more often than uniform sampling (2.89% of draws versus 0.06%).

Third, I improved the exploration strategy in the training loop. The original code adjusted the exploration rate (epsilon) with ad-hoc rules that snapped it to a fixed value whenever the win rate crossed a threshold. I replaced that with a single, explicit exponential epsilon-decay schedule (epsilon = max(eps_min, epsilon * eps_decay)) applied once per epoch, so exploration falls off smoothly and predictably and the schedule can be reported against convergence. Fourth, I made the work reproducible and measurable. I extracted the training loop out of the notebook into a clean, importable dqn_agent.py module, added a seed_everything() function that seeds Python, NumPy, and TensorFlow (and gave the buffer its own seeded random generator), and built a benchmark harness that returns the metrics my Module One plan called for: epochs to a stable 95% win rate, average steps-to-goal, and wall-clock training time. I also added a pure-Python unit-test suite (test_replay_buffer.py, six passing tests) that pins the data-structure behavior; FIFO eviction, capacity bounding, in-range sampling, correct sum-tree search, PER's over-sampling of high-error transitions, and reproducibility under a fixed seed.

A deliberate design decision runs through all of this. I kept the data-structure core (replay_buffer.py) completely free of any TensorFlow dependency. Only the two model-facing methods touch the neural network. That separation lets the algorithmic heart of the enhancement be tested and benchmarked on any modern Python interpreter, independent of the heavy ML framework, while the training path stays faithful to the original artifact.

### Alignment with Course Outcomes

This enhancement meets the course outcomes I identified for this category in my Module One plan, and my outcome-coverage plan for the algorithms category did not change.

- Outcome Three (design and evaluate computing solutions using algorithmic principles, while managing trade-offs): this is the primary outcome for this artifact. Selecting a ring buffer over a list is an algorithmic decision justified by complexity analysis (O(n) → O(1)), and prioritized replay is an algorithmic technique whose accuracy benefit I balanced against its added complexity by correcting the sampling bias with importance-sampling weights and by building the priorities on an O(log n) sum tree rather than an O(n) scan. The alpha/beta exponents that control prioritization strength and bias correction are explicit, documented trade-offs.

- Outcome Four (well-founded and innovative techniques, skills, and tools): I applied established, well-founded techniques from the reinforcement-learning literature (target networks, prioritized experience replay, epsilon-decay exploration) and standard data structures (deque ring buffer, sum tree) implemented with sound engineering: seeding for reproducibility, a decoupled testable core, and automated tests.

- Outcome Two (design and deliver professional-quality communication adapted to audience): the enhancement is documented for a technical reader through intent-bearing file headers and inline comments, the benchmark turns a claim of efficiency into evidence a reviewer can reproduce.

As in Enhancement One, the outcome this artifact does not fully carry on its own is Outcome One (collaboration and communicating to diverse audiences), which I am covering across the portfolio through the Module Two code review, the professional self-assessment, and professional repository artifacts such as the README and commit history. Outcome Five (security mindset) is carried primarily by the software-engineering and database enhancements rather than by this one.

### Reflection on the Process

The most valuable lesson from this enhancement was that a data-structure choice which looks trivial in isolation can dominate the cost of a whole training run once it sits on a hot path. Swapping a list for a deque(maxlen=N) is a one-line change, but because eviction ran on every step of every episode, that single line changed the buffer's behavior from scaling linearly with its size to being effectively constant. Watching the benchmark's speedup grow from about 10× to about 83× as I increased the capacity made the difference between O(n) and O(1) tangible in a way that reading about asymptotic complexity never quite did.

The main challenge was implementing prioritized replay correctly. The naive version, keep an array of priorities and rescan it on every sample, would have reintroduced exactly the kind of O(n) cost I had just removed. Getting the sum tree right so that both sampling and priority updates stayed O(log n), keeping its leaf indices synchronized with the ring buffer's eviction order, and then adding importance-sampling weights so the faster learning did not come at the price of a biased gradient, was the hardest and most rewarding part. I leaned on the same discipline that helped me in Enhancement One: create a clean seam so the tricky logic can be tested in isolation. Because I kept the buffer independent of TensorFlow, I could write fast unit tests that proved the eviction, sampling, and priority behavior were correct before ever attaching the neural network, which is also why I was able to verify this work in an environment where the full ML stack was not installable.

Measuring the payoff also taught me a lesson in honest evaluation. My first instinct was to assume prioritized replay would obviously converge faster than uniform sampling, but when I compared them on the original 8×8 maze the results were essentially a tie, and on a single seed they were noisy enough that either could appear to "win." Rather than tune the setup until PER looked better, I did two things. First, I confirmed the implementation was actually correct, an early version sampled by priority but never applied the importance-sampling weights to the loss, which biased training and made PER look *worse*; fixing that was what made the comparison fair. Second, I recognized why the maze was a hard case for PER: on a small maze the agent wins often, so informative transitions are not actually rare, and prioritized replay's advantage is largest precisely when success is rare and the buffer is large. I therefore evaluate on two maze sizes and average over multiple seeds, and I report whatever the data shows, including a tie on the small maze. The guaranteed, seed-independent result of this enhancement is the data-structure complexity improvement; whether prioritized replay measurably accelerates convergence is an empirical question that depends on the environment, and being precise about that distinction is itself part of designing and evaluating an algorithmic solution.
