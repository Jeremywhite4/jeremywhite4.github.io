# ENHANCEMENT 2 - Algorithms and Data Structures (CS-499 Capstone, Jeremy White)
# =============================================================================
# benchmark_replay.py - reproducible, TensorFlow-FREE evidence for the two
# algorithm/data-structure claims in this enhancement:
#
#   BENCHMARK 1 (data structure): full-buffer insertion cost.
#     Compares the ORIGINAL strategy (Python list + ``del lst[0]`` when full)
#     against the ENHANCED strategy (``collections.deque(maxlen=N)``) as the
#     buffer capacity N grows. The list is expected to scale ~O(n) because
#     deleting index 0 shifts every element; the deque should stay ~flat (O(1)).
#
#   BENCHMARK 2 (sampling quality): how quickly the rare, high-value transition
#     in a sparse buffer gets replayed under uniform vs prioritized sampling.
#     Demonstrates why PER accelerates learning in a sparse-reward maze.
#
# Run:  python benchmark_replay.py
# Only requires numpy (see requirements-core.txt).
# =============================================================================

import time
from collections import deque

import numpy as np

from replay_buffer import ReplayBuffer


# --------------------------------------------------------------- benchmark 1
def bench_insertion(capacities=(1000, 2000, 4000, 8000, 16000),
                    fills=3, seed=0):
    """Time appending into a FULL buffer for list-del0 vs deque(maxlen).

    For each capacity N we first fill the structure, then time ``fills * N``
    additional appends (every one of which triggers an eviction).
    """
    rng = np.random.default_rng(seed)
    print("Benchmark 1: full-buffer insertion cost (lower is better)")
    print(f"{'capacity N':>11} | {'list del[0] (s)':>16} | {'deque(maxlen) (s)':>18} | {'speedup':>8}")
    print("-" * 64)
    results = []
    for n in capacities:
        payload = [rng.random(64) for _ in range(1000)]  # reused sample states
        appends = fills * n

        # ORIGINAL: list with manual O(n) eviction at index 0.
        lst = [payload[i % len(payload)] for i in range(n)]
        t0 = time.perf_counter()
        for i in range(appends):
            lst.append(payload[i % len(payload)])
            if len(lst) > n:
                del lst[0]                       # O(n) shift
        t_list = time.perf_counter() - t0

        # ENHANCED: deque(maxlen) with O(1) auto-eviction.
        dq = deque((payload[i % len(payload)] for i in range(n)), maxlen=n)
        t0 = time.perf_counter()
        for i in range(appends):
            dq.append(payload[i % len(payload)])  # O(1), auto-evicts oldest
        t_deque = time.perf_counter() - t0

        speedup = t_list / t_deque if t_deque > 0 else float("inf")
        results.append((n, t_list, t_deque, speedup))
        print(f"{n:>11} | {t_list:>16.4f} | {t_deque:>18.4f} | {speedup:>7.1f}x")
    return results


# --------------------------------------------------------------- benchmark 2
def bench_sampling(capacity=2000, batch_size=32, draws=200, seed=0):
    """Compare uniform vs prioritized replay at surfacing a rare transition.

    We build a buffer of ``capacity`` transitions where exactly ONE has a large
    TD-error (the "informative" transition, e.g. reaching the treasure) and the
    rest are dull. We then repeatedly sample minibatches and measure how often
    the informative transition is drawn.
    """
    print("\nBenchmark 2: replay of the rare high-value transition")
    print(f"buffer={capacity}, batch={batch_size}, draws={draws}")

    special_id = capacity // 2

    def build(prioritized):
        buf = ReplayBuffer(capacity=capacity, prioritized=prioritized,
                           alpha=0.6, beta=0.4, seed=seed)
        for i in range(capacity):
            # transition marker is just its id here; PER only needs TD-errors.
            buf.append(i)
        if prioritized:
            # Assign one large TD-error to the special transition, tiny to rest.
            idx = np.arange(capacity)
            errs = np.full(capacity, 0.01, dtype=np.float64)
            errs[special_id] = 10.0
            buf.update_priorities(idx, errs)
        return buf

    def count_special(buf):
        hits = 0
        for _ in range(draws):
            batch, _idx, _w = buf.sample(batch_size)
            hits += sum(1 for t in batch if t == special_id)
        return hits

    uniform_hits = count_special(build(prioritized=False))
    per_hits = count_special(build(prioritized=True))
    total = draws * batch_size
    print(f"{'uniform':>12}: special sampled {uniform_hits:>5} / {total} "
          f"({100 * uniform_hits / total:.2f}%)")
    print(f"{'prioritized':>12}: special sampled {per_hits:>5} / {total} "
          f"({100 * per_hits / total:.2f}%)")
    factor = (per_hits / uniform_hits) if uniform_hits else float("inf")
    print(f"prioritized replays the informative transition ~{factor:.0f}x more often")
    return uniform_hits, per_hits


if __name__ == "__main__":
    bench_insertion()
    bench_sampling()
