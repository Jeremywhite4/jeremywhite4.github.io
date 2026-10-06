# ENHANCEMENT 2 - Algorithms and Data Structures (CS-499 Capstone, Jeremy White)
# =============================================================================
# Artifact:  Pirate Intelligent Agent (originally CS-370 Project Two, Aug 2026)
# File:      TreasureMaze.py  (the maze environment / MDP the agent learns in)
#
# WHAT CHANGED IN THIS ENHANCEMENT
# --------------------------------
# The environment's behaviour is intentionally preserved - the reward scheme,
# state representation, and win/lose rules are identical to the original CS-370
# artifact, because the enhancement targets the *learning algorithm and its data
# structures*, not the game. Two small, safe cleanups were made:
#
#   1. update_state() action dispatch clarified. The original mixed two separate
#      if/elif chains (LEFT/UP on one, RIGHT/DOWN on another). For mutually
#      exclusive actions the result was correct, but the control flow was easy to
#      misread. It is now a single if/elif/elif/elif over the four actions, which
#      is equivalent and unambiguous.
#
#   2. valid_actions() now returns a copy of a fresh list (it already did) and is
#      documented; get_reward() branches are documented. No logic change.
#
# The class remains free of any ML-framework dependency.
# =============================================================================

import numpy as np

visited_mark = 0.8  # Visited cells are marked with an 80% gray shade.
pirate_mark = 0.5   # The pirate's current cell is marked with a 50% gray shade.

# The agent can move in one of four directions.
LEFT = 0
UP = 1
RIGHT = 2
DOWN = 3


class TreasureMaze(object):
    """Grid-world maze environment for the deep Q-learning pirate agent.

    The maze is a 2-D NumPy array of floats in [0.0, 1.0]: 1.0 is a free cell,
    0.0 is an occupied (blocked) cell. The treasure sits at the bottom-right
    free cell. The agent is rewarded for reaching the treasure and penalized for
    invalid moves, revisiting cells, or getting blocked.
    """

    def __init__(self, maze, pirate=(0, 0)):
        self._maze = np.array(maze)
        nrows, ncols = self._maze.shape
        self.target = (nrows - 1, ncols - 1)   # treasure cell
        self.free_cells = [(r, c) for r in range(nrows) for c in range(ncols)
                           if self._maze[r, c] == 1.0]
        self.free_cells.remove(self.target)
        if self._maze[self.target] == 0.0:
            raise Exception("Invalid maze: target cell cannot be blocked!")
        if pirate not in self.free_cells:
            raise Exception("Invalid Pirate Location: must sit on a free cell")
        self.reset(pirate)

    def reset(self, pirate):
        """Reset the pirate to ``pirate`` and clear per-episode state."""
        self.pirate = pirate
        self.maze = np.copy(self._maze)
        row, col = pirate
        self.maze[row, col] = pirate_mark
        self.state = (row, col, 'start')
        # A floor on total reward stops episodes from running forever.
        self.min_reward = -0.5 * self.maze.size
        self.total_reward = 0
        self.visited = set()

    def update_state(self, action):
        """Advance the environment by one action.

        Modes: 'valid' (moved), 'invalid' (action not allowed, no move),
        'blocked' (no legal actions at all).
        """
        nrow, ncol, nmode = pirate_row, pirate_col, mode = self.state

        if self.maze[pirate_row, pirate_col] > 0.0:
            self.visited.add((pirate_row, pirate_col))  # mark visited

        valid_actions = self.valid_actions()

        if not valid_actions:
            nmode = 'blocked'
        elif action in valid_actions:
            nmode = 'valid'
            # ENHANCEMENT 2: single unambiguous dispatch over the four actions
            # (was two separate if/elif chains in the original).
            if action == LEFT:
                ncol -= 1
            elif action == UP:
                nrow -= 1
            elif action == RIGHT:
                ncol += 1
            elif action == DOWN:
                nrow += 1
        else:
            nmode = 'invalid'  # illegal action: pirate does not move

        self.state = (nrow, ncol, nmode)

    def get_reward(self):
        """Reward for the current state (range roughly [-1, 1])."""
        pirate_row, pirate_col, mode = self.state
        nrows, ncols = self.maze.shape
        if pirate_row == nrows - 1 and pirate_col == ncols - 1:
            return 1.0                      # reached the treasure
        if mode == 'blocked':
            return self.min_reward - 1      # dead end
        if (pirate_row, pirate_col) in self.visited:
            return -0.25                    # discourage wandering
        if mode == 'invalid':
            return -0.75                    # illegal move
        if mode == 'valid':
            return -0.04                    # small step cost

    def act(self, action):
        """Apply ``action``; return (envstate, reward, game_status)."""
        self.update_state(action)
        reward = self.get_reward()
        self.total_reward += reward
        status = self.game_status()
        envstate = self.observe()
        return envstate, reward, status

    def observe(self):
        """Return the flattened (1, N) environment state fed to the network."""
        canvas = self.draw_env()
        envstate = canvas.reshape((1, -1))
        return envstate

    def draw_env(self):
        """Render the maze to a grayscale canvas (free=1.0, pirate=0.5)."""
        canvas = np.copy(self.maze)
        nrows, ncols = self.maze.shape
        for r in range(nrows):
            for c in range(ncols):
                if canvas[r, c] > 0.0:
                    canvas[r, c] = 1.0
        row, col, valid = self.state
        canvas[row, col] = pirate_mark
        return canvas

    def game_status(self):
        """Return 'win', 'lose', or 'not_over' for the current state."""
        if self.total_reward < self.min_reward:
            return 'lose'
        pirate_row, pirate_col, mode = self.state
        nrows, ncols = self.maze.shape
        if pirate_row == nrows - 1 and pirate_col == ncols - 1:
            return 'win'
        return 'not_over'

    def valid_actions(self, cell=None):
        """Return the legal actions from ``cell`` (defaults to current cell)."""
        if cell is None:
            row, col, mode = self.state
        else:
            row, col = cell
        actions = [0, 1, 2, 3]
        nrows, ncols = self.maze.shape
        if row == 0:
            actions.remove(1)
        elif row == nrows - 1:
            actions.remove(3)

        if col == 0:
            actions.remove(0)
        elif col == ncols - 1:
            actions.remove(2)

        if row > 0 and self.maze[row - 1, col] == 0.0:
            actions.remove(1)
        if row < nrows - 1 and self.maze[row + 1, col] == 0.0:
            actions.remove(3)

        if col > 0 and self.maze[row, col - 1] == 0.0:
            actions.remove(0)
        if col < ncols - 1 and self.maze[row, col + 1] == 0.0:
            actions.remove(2)

        return actions
