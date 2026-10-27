-- Lineups (RF-24 to RF-29). Each manager has one working lineup per league; every save is an immutable version.
-- The lineup of a match is the last version saved before its lock (kickoff_at - lineup_lock_hours), so an
-- untouched lineup carries over by itself (RF-28).
CREATE TABLE lineup_version
(
  id        UUID                     NOT NULL PRIMARY KEY,
  league_id UUID                     NOT NULL, -- logical reference to league_league (no cross-module FK)
  user_id   UUID                     NOT NULL, -- logical reference to identity_user (no cross-module FK)
  saved_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX lineup_version_league_user_saved_idx ON lineup_version (league_id, user_id, saved_at DESC);

CREATE TABLE lineup_version_player
(
  version_id UUID NOT NULL REFERENCES lineup_version (id) ON DELETE CASCADE,
  player_id  UUID NOT NULL, -- logical reference to squad_player (no cross-module FK)
  PRIMARY KEY (version_id, player_id)
);

-- The lock of a match, materialized once by the lock job (or when the match is closed). Deleted if the match is
-- postponed with its lock back in the future, or deleted.
CREATE TABLE lineup_match_lock
(
  match_id   UUID                     NOT NULL PRIMARY KEY, -- logical reference to match_match (no cross-module FK)
  league_id  UUID                     NOT NULL,
  matchday   SMALLINT                 NOT NULL CHECK (matchday >= 0),
  kickoff_at TIMESTAMP WITH TIME ZONE NOT NULL,
  lock_at    TIMESTAMP WITH TIME ZONE NOT NULL,
  locked_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX lineup_match_lock_league_kickoff_idx ON lineup_match_lock (league_id, kickoff_at);

-- Each member's locked lineup, with the budget check made at the lock (RF-29): over budget scores 0 points.
CREATE TABLE lineup_locked_lineup
(
  match_id    UUID           NOT NULL REFERENCES lineup_match_lock (match_id) ON DELETE CASCADE,
  user_id     UUID           NOT NULL,
  version_id  UUID           NOT NULL REFERENCES lineup_version (id),
  total_value NUMERIC(10, 2) NOT NULL CHECK (total_value >= 0), -- millions
  budget      NUMERIC(10, 2) CHECK (budget >= 0), -- NULL = budget disabled
  over_budget BOOLEAN        NOT NULL,
  PRIMARY KEY (match_id, user_id)
);

-- Players' values at the lock, frozen against later market changes.
CREATE TABLE lineup_locked_player
(
  match_id  UUID           NOT NULL,
  user_id   UUID           NOT NULL,
  player_id UUID           NOT NULL,
  value     NUMERIC(10, 2) NOT NULL CHECK (value >= 0), -- millions
  PRIMARY KEY (match_id, user_id, player_id),
  FOREIGN KEY (match_id, user_id) REFERENCES lineup_locked_lineup (match_id, user_id) ON DELETE CASCADE
);
