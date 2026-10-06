# Enhancement Two — Algorithms and Data Structures
## Pirate Intelligent Agent (Deep Q-Learning)

**Author:** Jeremy White
**Course:** CS-499 Computer Science Capstone (SNHU)
**Original artifact:** CS-370 Project Two (August 2026) — a reinforcement-learning
pirate that learns to navigate an 8×8 maze to the treasure using a deep
Q-network with experience replay.

---

## What this enhancement does

The enhancement targets the **learning algorithm and its data structures**, not
the game itself. The maze environment behaves exactly as it did in CS-370; what
changed is *how the agent stores and learns from experience*.

| Area | Original | Enhanced |
|------|----------|----------|
| Replay buffer store | Python `list` | `collections.deque(maxlen=N)` ring buffer |
| Full-buffer eviction | `del memory[0]` — **O(n)** | auto-evict oldest — **O(1)** |
| Sampling | uniform random | **proportional prioritized replay (PER)** via a sum tree — O(log n) |
| Bias correction | none needed | importance-sampling weights |
| Exploration | ad-hoc epsilon nudges | single **exponential epsilon-decay** schedule |
| Reproducibility | unseeded | `seed_everything()` + seeded buffer RNG |
| Evaluation | win rate only | **benchmark harness**: epochs-to-95%, avg steps-to-goal, wall-clock |

## Files

| File | Purpose | Needs TensorFlow? |
|------|---------|:---:|
| `replay_buffer.py` | Ring buffer + `SumTree` for prioritized replay (the data-structure core) | No |
| `GameExperience.py` | Experience replay wrapper; same public API as the original | No* |
| `TreasureMaze.py` | Maze environment (behaviour preserved) | No |
| `dqn_agent.py` | DQN training loop, epsilon decay, seeding, metrics | Yes |
| `benchmark_replay.py` | TF-free evidence: list-vs-deque cost, uniform-vs-PER sampling | No |
| `test_replay_buffer.py` | Unit tests pinning the data-structure behaviour | No |

\* `GameExperience` imports TensorFlow lazily; only `predict()`/`get_data()` need a model.

## Design note — separating the algorithm from the framework

The data structures that this enhancement is really about (ring buffer, sum
tree, prioritized sampling, importance weights) are implemented in pure
Python/NumPy with **no TensorFlow dependency**. That seam is deliberate: it lets
the algorithmic core be unit-tested and benchmarked on any modern Python without
the heavy ML framework, while the neural-network training path in `dqn_agent.py`
stays faithful to the original CS-370 implementation.

## How to run

**Data-structure tests + benchmark (no TensorFlow, runs anywhere):**
```bash
pip install -r requirements-core.txt
python test_replay_buffer.py        # 6/6 tests pass
python benchmark_replay.py          # prints the numbers below
```

**Full training (requires TensorFlow 2.16 on Python 3.9–3.11, e.g. Codio/Colab):**
```bash
pip install -r requirements.txt
python dqn_agent.py                 # short demo run
# or, for the controlled comparison:
python -c "from dqn_agent import train, DEFAULT_MAZE; \
print(train(DEFAULT_MAZE, prioritized=False, seed=42).summary()); \
print(train(DEFAULT_MAZE, prioritized=True,  seed=42).summary())"
```

**Self-contained notebook (recommended — runs end to end in Google Colab):**
Open `Jeremy_White_ProjectTwo_Enhanced.ipynb` in Colab (TensorFlow is preinstalled)
and choose *Runtime → Run all*. All enhanced code is inline, so no extra files are
needed. Section 6 trains the agent with uniform vs prioritized replay from the same
seed and Section 7 plots the convergence chart (rolling win rate, and epochs-to-95%
for each). Raise `N_EPOCH` in Section 6 for a full solve; the default (250) keeps the
demo quick.

## Measured results (from `benchmark_replay.py`)

**Benchmark 1 — full-buffer insertion cost.** As capacity `N` grows, the list's
`del[0]` eviction scales linearly while the deque stays flat, confirming the
O(n) → O(1) improvement:

| capacity N | list `del[0]` | `deque(maxlen)` | speedup |
|-----------:|--------------:|----------------:|--------:|
| 1,000  | 0.0032 s | 0.0003 s | 10× |
| 2,000  | 0.0062 s | 0.0006 s | 10× |
| 4,000  | 0.0278 s | 0.0013 s | 22× |
| 8,000  | 0.1093 s | 0.0025 s | 44× |
| 16,000 | 0.4568 s | 0.0055 s | 83× |

**Benchmark 2 — replaying the rare, informative transition.** In a buffer of
2,000 transitions where one carries a large TD-error, prioritized replay sampled
that transition **~46× more often** than uniform sampling (2.89% vs 0.06% of
draws), which is why PER accelerates learning in a sparse-reward maze.

*(Exact timings vary by machine; the scaling trend and the PER ratio are the
point, and both are reproducible via a fixed seed.)*

**On convergence (honest note).** Whether prioritized replay *converges faster* than
uniform sampling is environment-dependent, not guaranteed. On the small 8×8 maze the
agent wins often, so informative transitions are not actually rare and PER tends to tie
uniform (and on a single seed the comparison is noisy). PER's advantage shows up in the
regime it is designed for — larger, sparser mazes with a large replay buffer — which is
why the notebook evaluates **two maze sizes across multiple seeds** and reports whatever
the data shows, including a tie. Two correctness points matter here: (1) the
importance-sampling weights must be applied to the loss, or prioritized sampling biases
training and can do *worse* than uniform; the notebook/`dqn_agent.py` apply them, and under
uniform mode every weight is 1.0 (identical to the original MSE loss). (2) The
data-structure complexity win above is independent of all of this.

## Complexity summary

For `n` stored transitions, sampling `k`:

| Operation | Original | Enhanced |
|-----------|----------|----------|
| append (buffer full) | O(n) | O(1) |
| uniform sample of k | O(k) | O(k) |
| prioritized sample of k | — | O(k log n) |
| priority update of k | — | O(k log n) |
