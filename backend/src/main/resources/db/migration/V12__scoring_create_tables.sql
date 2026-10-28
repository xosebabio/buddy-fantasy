-- Scoring (RF-15, RF-30 to RF-33). scoring keeps its own copy of each match's sheet (MatchClosed, MatchCorrected)
-- and locked lineups (LineupsLocked), and scores the match once it has both, in any order. Each copy keeps the
-- occurredAt of the event it came from: only a more recent event replaces it.
CREATE TABLE scoring_match
(
  match_id    UUID                     NOT NULL PRIMARY KEY, -- logical reference to match_match (no cross-module FK)
  league_id   UUID                     NOT NULL, -- logical reference to league_league (no cross-module FK)
  matchday    SMALLINT                 NOT NULL CHECK (matchday >= 0),
  kickoff_at  TIMESTAMP WITH TIME ZONE NOT NULL,
  sheet_at    TIMESTAMP WITH TIME ZONE, -- last MatchClosed or MatchCorrected applied; NULL = no sheet yet
  lineups_at  TIMESTAMP WITH TIME ZONE, -- LineupsLocked applied; NULL = no lineups (yet or after an unlock)
  unlocked_at TIMESTAMP WITH TIME ZONE, -- last LineupsUnlocked: an older LineupsLocked delivered late is ignored
  rules       JSONB, -- table of points frozen when the match is first scored; corrections reuse it
  scored_at   TIMESTAMP WITH TIME ZONE,
  CHECK ((rules IS NULL) = (scored_at IS NULL)),
  CHECK (scored_at IS NULL OR (sheet_at IS NOT NULL AND lineups_at IS NOT NULL))
);
CREATE INDEX scoring_match_league_kickoff_idx ON scoring_match (league_id, kickoff_at);

-- Copy of the sheet and points of every player in it, lined up or not (RF-11, RF-15, RF-30). The breakdown per
-- action is not stored: it is the sheet times the frozen rules.
CREATE TABLE scoring_player_score
(
  match_id         UUID     NOT NULL REFERENCES scoring_match (match_id) ON DELETE CASCADE,
  player_id        UUID     NOT NULL, -- logical reference to squad_player (no cross-module FK)
  league_id        UUID     NOT NULL,
  -- Snapshot of the sheet, not the player's current position.
  position         VARCHAR  NOT NULL CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD')),
  goals            SMALLINT NOT NULL CHECK (goals >= 0),
  penalty_goals    SMALLINT NOT NULL CHECK (penalty_goals >= 0),
  penalties_saved  SMALLINT NOT NULL CHECK (penalties_saved >= 0),
  penalties_missed SMALLINT NOT NULL CHECK (penalties_missed >= 0),
  assists          SMALLINT NOT NULL CHECK (assists >= 0),
  own_goals        SMALLINT NOT NULL CHECK (own_goals >= 0),
  yellow_card      BOOLEAN  NOT NULL,
  double_yellow    BOOLEAN  NOT NULL,
  red_card         BOOLEAN  NOT NULL,
  action_points    INTEGER, -- NULL until scored
  rating_points    INTEGER  NOT NULL DEFAULT 0, -- filled by rating (phase 2)
  points           INTEGER, -- action_points + rating_points; NULL until scored
  PRIMARY KEY (match_id, player_id),
  CHECK ((action_points IS NULL) = (points IS NULL)),
  CHECK (points = action_points + rating_points)
);
CREATE INDEX scoring_player_score_league_player_idx ON scoring_player_score (league_id, player_id);

-- Copy of each locked lineup and the manager's points (RF-31): over budget scores 0.
CREATE TABLE scoring_lineup
(
  match_id    UUID    NOT NULL REFERENCES scoring_match (match_id) ON DELETE CASCADE,
  user_id     UUID    NOT NULL, -- logical reference to identity_user (no cross-module FK)
  league_id   UUID    NOT NULL,
  over_budget BOOLEAN NOT NULL,
  points      INTEGER, -- NULL until scored
  PRIMARY KEY (match_id, user_id)
);
CREATE INDEX scoring_lineup_league_user_idx ON scoring_lineup (league_id, user_id);

CREATE TABLE scoring_lineup_player
(
  match_id  UUID NOT NULL,
  user_id   UUID NOT NULL,
  player_id UUID NOT NULL, -- logical reference to squad_player (no cross-module FK)
  PRIMARY KEY (match_id, user_id, player_id),
  FOREIGN KEY (match_id, user_id) REFERENCES scoring_lineup (match_id, user_id) ON DELETE CASCADE
);
