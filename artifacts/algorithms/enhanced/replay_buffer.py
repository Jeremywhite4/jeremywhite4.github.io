# ENHANCEMENT 2 - Algorithms and Data Structures (CS-499 Capstone, Jeremy White)
# =============================================================================
# replay_buffer.py - the data-structure core of the experience-replay upgrade.
#
# This module has NO TensorFlow dependency on purpose. It contains the two data
# structures the enhancement is really about, so they can be reasoned about,
# unit-tested, and benchmarked independently of the neural network:
#
#   * ReplayBuffer - a fixed-capacity ring buffer (collections.deque(maxlen=N))
#                    with an optional proportional prioritized-sampling mode.
#   * SumTree      - a binary heap-shaped array that stores sampling priorities
#                    and supports O(log n) update and prefix-sum search, which is
#                    what makes prioritized sampling efficient.
#
# COMPLEXITY SUMMARY (n = number of stored transitions)
# -----------------------------------------------------
#   append (buffer full):     list + del[0]  = O(n)   ->  deque(maxlen)  = O(1)
#   uniform sample of k:                                   O(k)
#   prioritized sample of k:   naive linear scan = O(n) ->  SumTree       = O(k log n)
#   priority update of k:      recompute total   = O(n) ->  SumTree       = O(k log n)
# =============================================================================

from collections import deque
import numpy as np


class SumTree:
    """Fixed-size sum tree for proportional prioritized sampling.

    Leaves hold non-negative priorities; each internal node holds the sum of its
    children. The root therefore holds the total priority. Sampling a value in
    [0, total) and walking down the tree lands on a leaf with probability
    proportional to that leaf's priority - all in O(log n).
    """

    def __init__(self, capacity):
        self.capacity = capacity
        # Internal nodes (capacity - 1) followed by capacity leaves.
        self.tree = np.zeros(2 * capacity - 1, dtype=np.float64)
        self.size = 0  # number of populated leaves (<= capacity)

    def total(self):
        """Total priority (root of the tree). O(1)."""
        return float(self.tree[0])

    def update(self, leaf_index, priority):
        """Set the priority of leaf ``leaf_index`` and propagate up. O(log n)."""
        tree_index = leaf_index + self.capacity - 1
        change = priority - self.tree[tree_index]
        self.tree[tree_index] = priority
        # Propagate the delta up to the root.
        parent = (tree_index - 1) // 2
        while True:
            self.tree[parent] += change
            if parent == 0:
                break
            parent = (parent - 1) // 2

    def get_leaf(self, value):
        """Find the leaf whose cumulative range contains ``value``. O(log n).

        Returns (leaf_index, priority).
        """
        parent = 0
        while True:
            left = 2 * parent + 1
            right = left + 1
            if left >= len(self.tree):  # reached a leaf
                leaf = parent
                break
            if value <= self.tree[left]:
                parent = left
            else:
                value -= self.tree[left]
                parent = right
        leaf_index = leaf - (self.capacity - 1)
        return leaf_index, float(self.tree[leaf])


class ReplayBuffer:
    """Ring-buffer experience replay with optional prioritized sampling.

    Parameters
    ----------
    capacity : int
        Maximum number of transitions retained. Oldest transitions are evicted
        first when the buffer is full (O(1) via ``deque(maxlen=capacity)``).
    prioritized : bool
        If False, sampling is uniform (the original behaviour). If True, sampling
        is proportional to each transition's stored priority (PER).
    alpha : float
        Priority exponent. 0 -> uniform even when prioritized=True; 1 -> full
        proportional prioritization.
    beta : float
        Importance-sampling exponent that corrects the bias introduced by
        non-uniform sampling. Typically annealed 0.4 -> 1.0 across training.
    epsilon : float
        Small constant added to every priority so no transition ever has zero
        probability of being sampled.
    seed : int or None
        Seed for the buffer's own RNG (reproducible sampling).
    """

    def __init__(self, capacity, prioritized=True, alpha=0.6, beta=0.4,
                 epsilon=1e-3, seed=None):
        self.capacity = int(capacity)
        self.prioritized = prioritized
        self.alpha = float(alpha)
        self.beta = float(beta)
        self.epsilon = float(epsilon)
        self._rng = np.random.default_rng(seed)

        # ENHANCEMENT 2: deque(maxlen) is the ring buffer. Append is O(1) and a
        # full buffer auto-evicts its oldest element with no shifting.
        self._data = deque(maxlen=self.capacity)

        if self.prioritized:
            self._tree = SumTree(self.capacity)
            self._write = 0                 # next leaf slot to write (circular)
            self._max_priority = 1.0        # priority assigned to new samples

    # --------------------------------------------------------------- container
    def __len__(self):
        return len(self._data)

    def __iter__(self):
        return iter(self._data)

    # ------------------------------------------------------------------ append
    def append(self, transition):
        """Store one transition in O(1).

        New transitions get the current maximum priority so they are guaranteed
        to be sampled (and re-prioritized by their real TD-error) at least once.
        """
        if not self.prioritized:
            self._data.append(transition)   # O(1) ring-buffer append
            return

        # Prioritized path: keep the deque and the sum tree in lockstep. The
        # leaf slot cycles 0..capacity-1 exactly like the deque's eviction order.
        if len(self._data) < self.capacity:
            self._data.append(transition)
        else:
            # Buffer full: deque evicts oldest; mirror that slot in the tree.
            self._data.append(transition)
        leaf = self._write
        priority = (self._max_priority + self.epsilon) ** self.alpha
        self._tree.update(leaf, priority)
        self._write = (self._write + 1) % self.capacity

    # ------------------------------------------------------------------ sample
    def sample(self, batch_size):
        """Sample a minibatch.

        Returns (transitions, indices, is_weights):
          * transitions - list of sampled transitions
          * indices     - leaf indices (for update_priorities); None if uniform
          * is_weights  - importance-sampling weights (all 1.0 if uniform)
        """
        n = len(self._data)
        if n == 0:
            return [], None, np.ones(0, dtype=np.float32)

        if not self.prioritized:
            # Original behaviour: uniform sampling, with replacement only if we
            # do not yet have enough distinct transitions.
            idx = self._rng.choice(n, size=batch_size, replace=(n < batch_size))
            batch = [self._data[i] for i in idx]
            return batch, None, np.ones(batch_size, dtype=np.float32)

        # Prioritized proportional sampling via the sum tree. We partition
        # [0, total) into batch_size equal segments and draw one sample per
        # segment - this spreads samples across the priority mass (stratified).
        total = self._tree.total()
        segment = total / batch_size
        indices = np.empty(batch_size, dtype=np.int64)
        priorities = np.empty(batch_size, dtype=np.float64)
        batch = []
        for i in range(batch_size):
            a, b = segment * i, segment * (i + 1)
            value = self._rng.uniform(a, b)
            leaf_index, priority = self._tree.get_leaf(value)
            leaf_index = min(leaf_index, n - 1)  # guard against empty leaves
            indices[i] = leaf_index
            priorities[i] = priority
            batch.append(self._data[leaf_index])

        # Importance-sampling weights: w_i = (1 / (N * P(i)))^beta, normalized by
        # the max weight so updates are only scaled down (training stability).
        probs = priorities / max(total, 1e-12)
        weights = (n * probs) ** (-self.beta)
        weights /= max(weights.max(), 1e-12)
        return batch, indices, weights.astype(np.float32)

    # -------------------------------------------------------- update_priorities
    def update_priorities(self, indices, td_errors):
        """Update priorities for previously sampled transitions. O(k log n).

        Called by GameExperience.get_data() after computing TD-errors. No-op in
        uniform mode.
        """
        if not self.prioritized or indices is None:
            return
        td_errors = np.abs(np.asarray(td_errors, dtype=np.float64))
        for idx, err in zip(indices, td_errors):
            priority = (err + self.epsilon) ** self.alpha
            self._tree.update(int(idx), priority)
            if err + self.epsilon > self._max_priority:
                self._max_priority = float(err + self.epsilon)
