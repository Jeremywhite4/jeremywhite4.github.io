# ENHANCEMENT 2 - Algorithms and Data Structures (CS-499 Capstone, Jeremy White)
# =============================================================================
# test_replay_buffer.py - pure-Python/NumPy unit tests for the replay buffer.
# No TensorFlow required. Run with:  python -m pytest test_replay_buffer.py
# or simply:  python test_replay_buffer.py  (falls back to a manual runner).
#
# These tests pin the DATA-STRUCTURE behaviour the enhancement depends on:
#   * the ring buffer never exceeds capacity and evicts oldest-first (FIFO);
#   * sampling only ever returns valid, in-range transitions;
#   * prioritized sampling strongly favours high-TD-error transitions;
#   * importance-sampling weights are in (0, 1] and reproducible under a seed.
# =============================================================================

import numpy as np

from replay_buffer import ReplayBuffer, SumTree


def test_ring_buffer_capacity_and_fifo_eviction():
    buf = ReplayBuffer(capacity=3, prioritized=False)
    for x in [10, 11, 12]:
        buf.append(x)
    assert len(buf) == 3
    assert list(buf) == [10, 11, 12]
    buf.append(13)                      # full -> evict oldest (10)
    assert len(buf) == 3                # never exceeds capacity
    assert list(buf) == [11, 12, 13]    # oldest-first eviction


def test_prioritized_capacity_bounded():
    buf = ReplayBuffer(capacity=5, prioritized=True, seed=1)
    for x in range(20):
        buf.append(x)
    assert len(buf) == 5                # bounded even in prioritized mode


def test_uniform_sampling_in_range():
    buf = ReplayBuffer(capacity=10, prioritized=False, seed=0)
    for x in range(10):
        buf.append(x)
    batch, idx, weights = buf.sample(5)
    assert len(batch) == 5
    assert idx is None                  # uniform mode returns no indices
    assert np.allclose(weights, 1.0)    # uniform IS weights are all 1
    assert all(0 <= t <= 9 for t in batch)


def test_prioritized_favours_high_td_error():
    # One transition (id=50) gets a huge TD-error; it should dominate sampling.
    cap = 100
    buf = ReplayBuffer(capacity=cap, prioritized=True, alpha=0.6, beta=0.4, seed=7)
    for x in range(cap):
        buf.append(x)
    errs = np.full(cap, 0.01)
    errs[50] = 20.0
    buf.update_priorities(np.arange(cap), errs)

    hits = 0
    draws = 300
    batch_size = 16
    for _ in range(draws):
        batch, idx, weights = buf.sample(batch_size)
        assert len(batch) == batch_size
        assert idx is not None
        assert np.all((weights > 0) & (weights <= 1.0 + 1e-6))
        hits += sum(1 for t in batch if t == 50)
    frac = hits / (draws * batch_size)
    # With uniform sampling this would be ~1/100 = 1%. PER should far exceed that.
    assert frac > 0.05, f"expected PER to over-sample the rare item, got {frac:.3f}"


def test_sumtree_total_and_search():
    tree = SumTree(4)
    for i, p in enumerate([1.0, 2.0, 3.0, 4.0]):
        tree.update(i, p)
    assert abs(tree.total() - 10.0) < 1e-9
    # value in [0,1) -> leaf 0; [1,3) -> leaf 1; [3,6) -> leaf 2; [6,10) -> leaf 3
    assert tree.get_leaf(0.5)[0] == 0
    assert tree.get_leaf(2.0)[0] == 1
    assert tree.get_leaf(5.0)[0] == 2
    assert tree.get_leaf(9.0)[0] == 3


def test_sampling_reproducible_with_seed():
    def draw():
        buf = ReplayBuffer(capacity=50, prioritized=True, seed=123)
        for x in range(50):
            buf.append(x)
        buf.update_priorities(np.arange(50), np.linspace(0.1, 5.0, 50))
        batch, idx, _ = buf.sample(10)
        return list(idx)
    assert draw() == draw()             # identical seed -> identical draw


if __name__ == "__main__":
    # Minimal runner so the file works without pytest installed.
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    passed = 0
    for t in tests:
        t()
        print(f"PASS  {t.__name__}")
        passed += 1
    print(f"\n{passed}/{len(tests)} tests passed")
