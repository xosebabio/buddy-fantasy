-- Ratings (RF-17 to RF-23, RF-41). A window per closed match opens with its MatchClosed and closes rating_window_hours
-- later. It freezes the league's rating parameters when it opens: later edits only affect matches closed after.
CREATE TABLE rating_window
(
  match_id          UUID                     NOT NULL PRIMARY KEY, -- logical reference to match_match (no cross-module FK)
  league_id         UUID                     NOT NULL, -- logical reference to league_league (no cross-module FK)
  matchday          SMALLINT                 NOT NULL CHECK (matchday >= 0),
  kickoff_at        TIMESTAMP WITH TIME ZONE NOT NULL,
  opened_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  closes_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  closed_at         TIMESTAMP WITH TIME ZONE, -- NULL until the job closes it
  -- Parameters frozen when the window opens: points = conversion_factor x average + conversion_base.
  format            VARCHAR                  NOT NULL CHECK (format IN ('SPADES', 'DECIMAL')),
  conversion_factor NUMERIC(6, 2)            NOT NULL CHECK (conversion_factor > 0),
  conversion_base   NUMERIC(6, 2)            NOT NULL,
  min_votes         INTEGER                  NOT NULL CHECK (min_votes >= 1),
  default_rating    NUMERIC(4, 2) CHECK (default_rating >= 0), -- NULL = unrated players score 0
  sheet_at          TIMESTAMP WITH TIME ZONE NOT NULL, -- last MatchClosed or MatchCorrected applied
  CHECK (closes_at > opened_at),
  CHECK (closed_at IS NULL OR closed_at >= closes_at)
);
CREATE INDEX rating_window_due_idx ON rating_window (closes_at) WHERE closed_at IS NULL;
CREATE INDEX rating_window_league_kickoff_idx ON rating_window (league_id, kickoff_at);

-- The players that can be rated, a copy of the sheet, and their result frozen when the window closes.
CREATE TABLE rating_player
(
  match_id      UUID    NOT NULL REFERENCES rating_window (match_id) ON DELETE CASCADE,
  player_id     UUID    NOT NULL, -- logical reference to squad_player (no cross-module FK)
  -- Snapshot of the sheet, not the player's current position.
  position      VARCHAR NOT NULL CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD')),
  votes         INTEGER CHECK (votes >= 0), -- NULL until closed
  average       NUMERIC(4, 2) CHECK (average >= 0), -- only to show; NULL until closed or without votes
  rating_points INTEGER, -- NULL until closed
  PRIMARY KEY (match_id, player_id),
  CHECK ((votes IS NULL) = (rating_points IS NULL)),
  CHECK ((average IS NULL) = (votes IS NULL OR votes = 0))
);

-- Each member's votes (RF-18 to RF-21): anonymous, never returned to anyone but their author. Removing a player from
-- the sheet removes his votes.
CREATE TABLE rating_vote
(
  match_id   UUID                     NOT NULL,
  player_id  UUID                     NOT NULL,
  user_id    UUID                     NOT NULL, -- logical reference to identity_user (no cross-module FK)
  value      SMALLINT                 NOT NULL CHECK (value >= 0), -- the upper bound depends on the window's format
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (match_id, player_id, user_id),
  FOREIGN KEY (match_id, player_id) REFERENCES rating_player (match_id, player_id) ON DELETE CASCADE
);
CREATE INDEX rating_vote_match_user_idx ON rating_vote (match_id, user_id);

-- Members whose ratings of a match the admin cancelled (RF-41): they can no longer rate it. It is also the audit of
-- who cancelled whom and when.
CREATE TABLE rating_exclusion
(
  match_id    UUID                     NOT NULL REFERENCES rating_window (match_id) ON DELETE CASCADE,
  user_id     UUID                     NOT NULL, -- logical reference to identity_user (no cross-module FK)
  excluded_by UUID                     NOT NULL, -- the admin
  excluded_at TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (match_id, user_id)
);
