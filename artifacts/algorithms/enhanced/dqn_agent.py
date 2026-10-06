# ENHANCEMENT 2 - Algorithms and Data Structures (CS-499 Capstone, Jeremy White)
# =============================================================================
# dqn_agent.py - the deep Q-learning training loop, extracted from the original
# CS-370 notebook (qtrain) into a clean, importable, reproducible module.
#
# WHAT CHANGED IN THIS ENHANCEMENT
# --------------------------------
#   1. PRINCIPLED EPSILON-DECAY SCHEDULE. The original loop nudged epsilon with
#      ad-hoc rules (snap to 0.05 when win_rate > 0.9, otherwise multiply). This
#      module uses a single, explicit exponential decay applied once per epoch:
#          epsilon = max(EPS_MIN, epsilon * EPS_DECAY)
#      so exploration falls off smoothly and the schedule is easy to reason about
#      and to report against convergence.
#
#   2. REPRODUCIBILITY. seed_everything() seeds Python, NumPy, and TensorFlow so
#      a run can be repeated and compared. The replay buffer takes its own seed.
#
#   3. INSTRUMENTED BENCHMARK. train() returns a TrainResult dataclass with the
#      metrics the narrative reports: epochs to a stable >=95% win rate, average
#      steps-to-goal over an evaluation sweep, and wall-clock training time.
#
#   4. DATA-STRUCTURE TOGGLE. The experience buffer is the enhanced deque-backed
#      GameExperience, and prioritized replay can be switched on/off to produce a
#      controlled before/after comparison.
#
# TensorFlow IS required to run this module (as in the original). The pure data
# -structure logic lives in replay_buffer.py and is tested/benchmarked without TF.
# =============================================================================

from __future__ import print_function
import os
import random
import time
from dataclasses import dataclass, field
from typing import List, Optional

import numpy as np

from TreasureMaze import TreasureMaze
from GameExperience import GameExperience

LEFT, UP, RIGHT, DOWN = 0, 1, 2, 3
num_actions = 4


# ------------------------------------------------------------------ reproducible
def seed_everything(seed: int) -> None:
    """Seed Python, NumPy, and TensorFlow (if present) for repeatable runs."""
    os.environ["PYTHONHASHSEED"] = str(seed)
    random.seed(seed)
    np.random.seed(seed)
    try:
        import tensorflow as tf
        tf.random.set_seed(seed)
    except Exception:
        pass


# ------------------------------------------------------------------- model build
def build_model(maze: np.ndarray):
    """Build the Q-network. Same architecture as the original artifact."""
    import tensorflow as tf
    from tensorflow.keras.models import Sequential
    from tensorflow.keras.layers import Dense, PReLU, Input
    model = Sequential()
    model.add(Input(shape=(maze.size,)))
    model.add(Dense(maze.size))
    model.add(PReLU())
    model.add(Dense(maze.size))
    model.add(PReLU())
    model.add(Dense(num_actions))
    model.compile(optimizer="adam", loss="mse")
    return model


# ---------------------------------------------------------------------- rollout
def play_game(model, qmaze: TreasureMaze, pirate_cell, max_steps=None):
    """Greedily roll out one game; return (won: bool, steps: int)."""
    qmaze.reset(pirate_cell)
    envstate = qmaze.observe()
    steps = 0
    if max_steps is None:
        max_steps = qmaze.maze.size * 4
    while steps < max_steps:
        state = np.asarray(envstate, dtype=np.float32)
        if state.ndim == 1:
            state = np.expand_dims(state, axis=0)
        q_values = model(state, training=False).numpy()
        action = int(np.argmax(q_values[0]))
        envstate, reward, game_status = qmaze.act(action)
        steps += 1
        if game_status == "win":
            return True, steps
        if game_status == "lose":
            return False, steps
    return False, steps


def completion_check(model, qmaze: TreasureMaze):
    """True only if the agent wins from every reachable free starting cell."""
    for cell in qmaze.free_cells:
        if not qmaze.valid_actions(cell):
            continue
        won, _ = play_game(model, qmaze, cell)
        if not won:
            return False
    return True


def evaluate_avg_steps(model, qmaze: TreasureMaze) -> float:
    """Average steps-to-goal over all winnable starting cells (post-training)."""
    steps_list = []
    for cell in qmaze.free_cells:
        if not qmaze.valid_actions(cell):
            continue
        won, steps = play_game(model, qmaze, cell)
        if won:
            steps_list.append(steps)
    return float(np.mean(steps_list)) if steps_list else float("nan")


# ------------------------------------------------------------------ result type
@dataclass
class TrainResult:
    prioritized: bool
    seed: int
    epochs_run: int
    epochs_to_target: Optional[int]      # first epoch reaching stable >=95% win rate
    solved: bool                         # passed completion_check
    avg_steps_to_goal: float
    wall_clock_sec: float
    win_history: List[int] = field(default_factory=list)

    def summary(self) -> str:
        tgt = self.epochs_to_target if self.epochs_to_target is not None else "n/a"
        return (f"[{'PER' if self.prioritized else 'uniform'} seed={self.seed}] "
                f"epochs_run={self.epochs_run} epochs_to_95%={tgt} "
                f"solved={self.solved} avg_steps={self.avg_steps_to_goal:.2f} "
                f"time={self.wall_clock_sec:.1f}s")


# ---------------------------------------------------------------------- training
def train(maze: np.ndarray,
          n_epoch: int = 1000,
          max_memory: int = 1000,
          batch_size: int = 50,
          target_update_freq: int = 50,
          eps_start: float = 1.0,
          eps_min: float = 0.05,
          eps_decay: float = 0.995,
          discount: float = 0.95,
          prioritized: bool = True,
          win_rate_target: float = 0.95,
          seed: int = 42,
          verbose: bool = True) -> TrainResult:
    """Train the DQN pirate agent and return benchmark metrics.

    The loop mirrors the original qtrain(), but with a clean epsilon schedule,
    the deque/PER buffer, seeding, and metric capture.
    """
    import tensorflow as tf
    from tensorflow.keras.models import clone_model

    seed_everything(seed)

    qmaze = TreasureMaze(maze)
    model = build_model(maze)
    target_model = clone_model(model)
    target_model.set_weights(model.get_weights())

    experience = GameExperience(
        model, target_model, max_memory=max_memory,
        discount=discount, prioritized=prioritized,
    )
    # Let the buffer's sampling RNG be reproducible too.
    experience.buffer._rng = np.random.default_rng(seed)

    optimizer = tf.keras.optimizers.Adam()

    @tf.function
    def train_step(x, y, sample_weight):
        with tf.GradientTape() as tape:
            q_values = model(x, training=True)
            # ENHANCEMENT 2: weight each sample's squared error by its
            # importance-sampling weight. This is the bias-correction half of
            # prioritized replay - without it, prioritized sampling trains on a
            # skewed distribution and can perform no better (or worse) than
            # uniform. Under uniform replay every weight is 1.0, so this reduces
            # to the original mean-squared-error loss.
            per_sample = tf.reduce_mean(tf.square(y - q_values), axis=1)
            loss = tf.reduce_mean(sample_weight * per_sample)
        grads = tape.gradient(loss, model.trainable_variables)
        optimizer.apply_gradients(zip(grads, model.trainable_variables))
        return loss

    epsilon = eps_start
    win_history: List[int] = []
    hsize = qmaze.maze.size // 2
    epochs_to_target: Optional[int] = None
    solved = False
    start = time.time()

    for epoch in range(n_epoch):
        agent_cell = random.choice(qmaze.free_cells)
        qmaze.reset(agent_cell)
        envstate = qmaze.observe()
        game_over = False

        while not game_over:
            prev_envstate = envstate
            if np.random.rand() < epsilon:
                valid = qmaze.valid_actions()
                action = random.choice(valid) if valid else random.randrange(num_actions)
            else:
                action = int(np.argmax(experience.predict(prev_envstate)))

            envstate, reward, game_status = qmaze.act(action)
            if game_status == "win":
                win_history.append(1)
                game_over = True
            elif game_status == "lose":
                win_history.append(0)
                game_over = True

            experience.remember([prev_envstate, action, reward, envstate, game_over])
            inputs, targets = experience.get_data(batch_size)
            if inputs is not None:
                # IS weights are all 1.0 in uniform mode, so both paths share
                # this identical call and only differ by the sampling/weighting.
                weights = getattr(experience, "last_is_weights", None)
                if weights is None or len(weights) != len(inputs):
                    weights = np.ones(len(inputs), dtype=np.float32)
                train_step(
                    tf.convert_to_tensor(inputs),
                    tf.convert_to_tensor(targets),
                    tf.convert_to_tensor(weights, dtype=tf.float32),
                )

        # Target network refresh on a fixed cadence.
        if epoch % target_update_freq == 0:
            target_model.set_weights(model.get_weights())

        # ENHANCEMENT 2: single, smooth exponential epsilon decay per epoch.
        epsilon = max(eps_min, epsilon * eps_decay)

        win_rate = sum(win_history[-hsize:]) / hsize if len(win_history) >= hsize else 0.0
        if verbose and epoch % 10 == 0:
            print(f"Epoch {epoch:03d}/{n_epoch - 1} | eps {epsilon:.3f} | "
                  f"wins {sum(win_history)} | win_rate {win_rate:.3f}")

        if epochs_to_target is None and win_rate >= win_rate_target:
            epochs_to_target = epoch
        if win_rate >= 0.999 and completion_check(model, qmaze):
            solved = True
            if verbose:
                print(f"Solved: 100% win rate at epoch {epoch}")
            break

    wall = time.time() - start
    avg_steps = evaluate_avg_steps(model, qmaze)
    return TrainResult(
        prioritized=prioritized, seed=seed, epochs_run=epoch + 1,
        epochs_to_target=epochs_to_target, solved=solved,
        avg_steps_to_goal=avg_steps, wall_clock_sec=wall, win_history=win_history,
    )


# 8x8 maze from the original artifact, kept as the default benchmark environment.
DEFAULT_MAZE = np.array([
    [1., 0., 1., 1., 1., 1., 1., 1.],
    [1., 0., 1., 1., 1., 0., 1., 1.],
    [1., 1., 1., 1., 0., 1., 0., 1.],
    [1., 1., 1., 0., 1., 1., 1., 1.],
    [1., 1., 0., 1., 1., 1., 1., 1.],
    [1., 1., 1., 0., 1., 0., 0., 0.],
    [1., 1., 1., 0., 1., 1., 1., 1.],
    [1., 1., 1., 1., 0., 1., 1., 1.],
])


def make_maze(size: int, seed: int = 0, extra_open: float = 0.12) -> np.ndarray:
    """Generate a solvable square maze of the given size with real walls.

    Uses randomized-DFS "wall carving" on the standard (2k+1) lattice where walls
    live *between* cells, guaranteeing a connected path from the top-left start to
    the bottom-right treasure while leaving genuine obstacles. A small fraction of
    extra walls is then reopened so the agent has alternative routes. Larger mazes
    with real walls make a successful episode rare early in training, which is
    exactly the sparse-reward regime where prioritized replay is expected to help.
    """
    rng = np.random.default_rng(seed)
    # Work on a (size x size) grid of cells; walls sit between them. We build at
    # the cell level and connect neighbors by opening the wall between them.
    # Represent as full grid: 0 = wall, 1 = free. Cells at even coordinates.
    n = size if size % 2 == 1 else size - 1  # carving needs odd dimension
    grid = np.zeros((n, n), dtype=float)

    def cell_neighbors(r, c):
        for dr, dc in ((-2, 0), (2, 0), (0, -2), (0, 2)):
            nr, nc = r + dr, c + dc
            if 0 <= nr < n and 0 <= nc < n:
                yield nr, nc, r + dr // 2, c + dc // 2

    stack = [(0, 0)]
    grid[0, 0] = 1.0
    visited = {(0, 0)}
    while stack:
        r, c = stack[-1]
        opts = [(nr, nc, wr, wc) for nr, nc, wr, wc in cell_neighbors(r, c)
                if (nr, nc) not in visited]
        if not opts:
            stack.pop()
            continue
        nr, nc, wr, wc = opts[rng.integers(len(opts))]
        grid[nr, nc] = 1.0     # open the neighbor cell
        grid[wr, wc] = 1.0     # open the wall between them
        visited.add((nr, nc))
        stack.append((nr, nc))

    # Reopen a small fraction of interior walls for alternative routes.
    walls = np.argwhere(grid == 0.0)
    if len(walls) and extra_open > 0:
        rng.shuffle(walls)
        for r, c in walls[: int(extra_open * len(walls))]:
            grid[r, c] = 1.0

    # Pad to requested size if we reduced to odd n, keeping start/target free.
    if n != size:
        padded = np.zeros((size, size), dtype=float)
        padded[:n, :n] = grid
        # connect the padded border row/col so the target stays reachable
        padded[n - 1, :] = 1.0
        padded[:, n - 1] = 1.0
        grid = padded

    grid[0, 0] = 1.0
    grid[size - 1, size - 1] = 1.0
    return grid


def compare(maze: np.ndarray, seeds=(42, 7, 123), **train_kwargs):
    """Run uniform vs prioritized replay across multiple seeds.

    Returns (uniform_results, per_results) as lists of TrainResult. Averaging
    over seeds is essential: a single RL run is too noisy to compare two
    configurations fairly.
    """
    uniform, per = [], []
    for s in seeds:
        uniform.append(train(maze, prioritized=False, seed=s, verbose=False, **train_kwargs))
        per.append(train(maze, prioritized=True, seed=s, verbose=False, **train_kwargs))
    return uniform, per


def summarize_runs(results: List["TrainResult"], label: str) -> str:
    """Mean +/- spread over seeds for the key metrics (ignores unsolved for steps)."""
    hit = [r.epochs_to_target for r in results if r.epochs_to_target is not None]
    steps = [r.avg_steps_to_goal for r in results if not np.isnan(r.avg_steps_to_goal)]
    solved = sum(1 for r in results if r.solved)
    mean_to = f"{np.mean(hit):.0f}" if hit else "n/a"
    reached = f"{len(hit)}/{len(results)}"
    mean_steps = f"{np.mean(steps):.2f}" if steps else "n/a"
    return (f"{label:>10}: reached95%={reached} runs | mean_epochs_to_95%={mean_to} | "
            f"mean_avg_steps={mean_steps} | solved={solved}/{len(results)}")


if __name__ == "__main__":
    # Small demonstration run (requires TensorFlow).
    result = train(DEFAULT_MAZE, n_epoch=200, prioritized=True, seed=42)
    print(result.summary())
