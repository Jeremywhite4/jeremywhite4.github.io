# ENHANCEMENT 2 - Algorithms and Data Structures (CS-499 Capstone, Jeremy White)
# =============================================================================
# Artifact:  Pirate Intelligent Agent (originally CS-370 Project Two, Aug 2026)
# File:      GameExperience.py  (experience-replay buffer for deep Q-learning)
#
# WHAT CHANGED IN THIS ENHANCEMENT
# --------------------------------
# The original replay buffer stored episodes in a plain Python ``list`` and
# evicted the oldest episode with ``del self.memory[0]`` once the buffer was
# full. Deleting index 0 of a list is O(n): CPython must shift every remaining
# element left by one slot. Because ``remember()`` is called on every single
# step of every training episode, that O(n) eviction ran tens of thousands of
# times per training run.
#
# This enhancement makes two algorithm/data-structure improvements:
#
#   1. RING BUFFER (O(1) eviction).
#      The backing store is now ``collections.deque(maxlen=N)``. A deque is a
#      doubly linked list of fixed-size blocks, so appends and pops at either
#      end are O(1). With ``maxlen`` set, appending to a full deque atomically
#      evicts the oldest element - no manual length check, no element shifting.
#      Complexity of the full-buffer append drops from O(n) to O(1).
#
#   2. PROPORTIONAL PRIORITIZED EXPERIENCE REPLAY (PER).
#      The original ``sample()`` drew episodes uniformly at random. In a sparse
#      -reward maze the vast majority of transitions are dull single-step moves;
#      the rare, informative transitions (reaching the treasure, hitting a wall)
#      are sampled just as infrequently as everything else, so learning is slow.
#      PER instead samples a transition with probability proportional to its
#      last temporal-difference (TD) error - the transitions the network is most
#      "surprised" by are replayed more often. Because biased sampling would
#      distort the expected gradient, each sampled transition carries an
#      importance-sampling (IS) weight that corrects the bias. New transitions
#      are inserted at maximum priority so every experience is trained on at
#      least once.
#
# Both behaviours are preserved behind the SAME public interface the notebook
# already calls (``remember`` / ``predict`` / ``get_data``), so the training
# loop did not have to change to adopt them. Uniform sampling is still available
# by constructing the buffer with ``prioritized=False``, which is what the
# benchmark harness uses for a controlled before/after comparison.
#
# DEPENDENCY NOTE
# ---------------
# The priority math (SumTree, IS weights, sampling) is intentionally pure
# NumPy/Python with NO TensorFlow dependency, so the data-structure logic can be
# unit-tested and benchmarked off the ML framework (see benchmark_replay.py).
# Only ``predict`` and ``get_data`` touch the Keras model, exactly as before.
# =============================================================================

import numpy as np

try:
    # TensorFlow is only needed for the model-facing helpers (predict/get_data).
    # Importing lazily keeps the data-structure core usable without TF installed.
    import tensorflow as tf  # noqa: F401
    _TF_AVAILABLE = True
except Exception:  # pragma: no cover - environment without TF
    _TF_AVAILABLE = False

from replay_buffer import ReplayBuffer


class GameExperience(object):
    """Deep Q-learning experience replay.

    Backed by a fixed-capacity ring buffer (``collections.deque(maxlen=N)``)
    with optional proportional prioritized sampling. Public interface is
    unchanged from the original CS-370 artifact:

        remember(episode)          -> store one transition,       O(1)
        predict(envstate)          -> greedy Q-values for a state
        get_data(batch_size)       -> (inputs, targets) minibatch

    Parameters
    ----------
    model, target_model : Keras models (online + target networks).
    max_memory : int   capacity of the ring buffer.
    discount   : float reward discount factor (gamma).
    prioritized : bool  True -> prioritized replay, False -> uniform (original).
    alpha, beta : PER exponents (priority sharpness / IS-correction strength).
    """

    def __init__(self, model, target_model, max_memory=100, discount=0.95,
                 prioritized=True, alpha=0.6, beta=0.4):
        self.model = model
        self.target_model = target_model
        self.max_memory = max_memory
        self.discount = discount
        self.num_actions = model.output_shape[-1] if model is not None else 4
        # ENHANCEMENT 2: deque-backed ring buffer replaces the plain list.
        self.buffer = ReplayBuffer(
            capacity=max_memory, prioritized=prioritized, alpha=alpha, beta=beta
        )
        # Indices of the transitions returned by the most recent get_data() call,
        # so the training loop can feed TD-errors back via update_priorities().
        self._last_batch_indices = None

    # ------------------------------------------------------------------ store
    def remember(self, episode):
        """Store one transition. O(1) append with automatic oldest-eviction.

        episode = [envstate, action, reward, envstate_next, game_over]
        envstate == flattened 1d maze cells (see TreasureMaze.observe()).
        """
        # ENHANCEMENT 2: was ``self.memory.append(...)`` + ``del self.memory[0]``
        # (O(n) eviction). The ring buffer evicts the oldest element in O(1).
        self.buffer.append(episode)

    @property
    def memory(self):
        """Back-compat view: expose stored transitions as a list.

        Some of the original notebook/debug code referenced ``experience.memory``
        directly. Kept as a read-only convenience; not used on the hot path.
        """
        return list(self.buffer)

    def __len__(self):
        return len(self.buffer)

    # ---------------------------------------------------------------- predict
    def predict(self, envstate):
        """Greedy Q-values for a single environment state."""
        envstate = np.asarray(envstate, dtype=np.float32)
        if envstate.ndim == 1:
            envstate = np.expand_dims(envstate, axis=0)
        return self.model(envstate, training=False).numpy()[0]

    # ----------------------------------------------------------------- sample
    def sample(self, batch_size=32):
        """Sample a batch of transitions.

        Returns the transitions only (indices/weights are retained internally
        for the prioritized path). Kept for interface compatibility with the
        original class.
        """
        batch, indices, _weights = self.buffer.sample(batch_size)
        self._last_batch_indices = indices
        return batch

    # --------------------------------------------------------------- get_data
    def get_data(self, batch_size=32):
        """Build one training minibatch (inputs, targets) from replay.

        Uses the target network for the bootstrap term, exactly as the original
        implementation did. When prioritized replay is active this also computes
        per-sample TD-errors and updates their priorities so future sampling
        focuses on the transitions the network still gets wrong.
        """
        if self.model is None or self.target_model is None:
            raise ValueError("Both `model` and `target_model` must be provided")
        if len(self.buffer) == 0:
            return None, None

        batch, indices, is_weights = self.buffer.sample(batch_size)
        self._last_batch_indices = indices
        n = len(batch)
        env_size = np.asarray(batch[0][0]).reshape(1, -1).shape[1]

        inputs = np.zeros((n, env_size), dtype=np.float32)
        targets = np.zeros((n, self.num_actions), dtype=np.float32)

        states = np.vstack([np.asarray(b[0]).reshape(1, -1) for b in batch])
        next_states = np.vstack([np.asarray(b[3]).reshape(1, -1) for b in batch])

        q_values = self.model(states, training=False).numpy()
        q_next = self.target_model(next_states, training=False).numpy()

        td_errors = np.zeros(n, dtype=np.float32)
        for i, (state, action, reward, next_state, done) in enumerate(batch):
            inputs[i] = np.asarray(state).reshape(-1)
            targets[i] = q_values[i]
            if done:
                new_q = reward
            else:
                new_q = reward + self.discount * np.max(q_next[i])
            td_errors[i] = new_q - targets[i, action]
            targets[i, action] = new_q

        # ENHANCEMENT 2: feed TD-errors back as new priorities so the most
        # informative transitions are replayed more often next time.
        self.buffer.update_priorities(indices, np.abs(td_errors))

        # Expose IS weights for callers that support weighted loss. The default
        # training path can ignore them (uniform weights when prioritized=False).
        self.last_is_weights = is_weights
        return inputs, targets
