-- Per-league parameters (RF-07). Money values (player value, budget) are in millions.
CREATE TABLE league_settings
(
  league_id                UUID          NOT NULL PRIMARY KEY REFERENCES league_league (id),
  rating_window_hours      INTEGER       NOT NULL CHECK (rating_window_hours >= 1),
  rating_format            VARCHAR(10)   NOT NULL CHECK (rating_format IN ('SPADES', 'DECIMAL')),
  rating_points_per_unit   NUMERIC(6, 2) NOT NULL CHECK (rating_points_per_unit > 0),
  default_rating           NUMERIC(4, 2) CHECK (default_rating >= 0 AND default_rating <= 10), -- NULL = no default
  lineup_lock_hours        INTEGER       NOT NULL CHECK (lineup_lock_hours >= 0),
  lineup_max_changes       INTEGER       NOT NULL CHECK (lineup_max_changes >= 0),
  lineup_size              INTEGER       NOT NULL CHECK (lineup_size >= 1),
  lineup_formation_enabled BOOLEAN       NOT NULL,
  lineup_budget_enabled    BOOLEAN       NOT NULL,
  lineup_budget            NUMERIC(10, 2) NOT NULL CHECK (lineup_budget > 0),
  market_k                 NUMERIC(6, 4) NOT NULL CHECK (market_k > 0),
  market_max_change_pct    NUMERIC(5, 2) NOT NULL CHECK (market_max_change_pct > 0 AND market_max_change_pct <= 100),
  market_min_value         NUMERIC(10, 2) NOT NULL CHECK (market_min_value > 0)
);

-- Points per action and position: 9 actions x 4 positions per league.
CREATE TABLE league_scoring_rule
(
  league_id UUID        NOT NULL REFERENCES league_league (id),
  action    VARCHAR(20) NOT NULL CHECK (action IN ('GOAL', 'PENALTY_GOAL', 'PENALTY_SAVED', 'PENALTY_MISSED', 'ASSIST',
                                                    'OWN_GOAL', 'YELLOW_CARD', 'DOUBLE_YELLOW', 'RED_CARD')),
  position  VARCHAR(20) NOT NULL CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD')),
  points    INTEGER     NOT NULL,
  PRIMARY KEY (league_id, action, position)
);

-- Backfill leagues created before this migration with the default parameters.
INSERT INTO league_settings (league_id, rating_window_hours, rating_format, rating_points_per_unit, default_rating,
                             lineup_lock_hours, lineup_max_changes, lineup_size, lineup_formation_enabled,
                             lineup_budget_enabled, lineup_budget, market_k, market_max_change_pct, market_min_value)
SELECT id, 24, 'SPADES', 1, NULL, 2, 2, 7, FALSE, FALSE, 100, 0.05, 10, 1
FROM league_league;

INSERT INTO league_scoring_rule (league_id, action, position, points)
SELECT l.id, r.action, r.position, r.points
FROM league_league l
       CROSS JOIN (VALUES ('GOAL', 'GOALKEEPER', 6), ('GOAL', 'DEFENDER', 5), ('GOAL', 'MIDFIELDER', 4), ('GOAL', 'FORWARD', 3),
                          ('PENALTY_GOAL', 'GOALKEEPER', 3), ('PENALTY_GOAL', 'DEFENDER', 3), ('PENALTY_GOAL', 'MIDFIELDER', 3), ('PENALTY_GOAL', 'FORWARD', 3),
                          ('PENALTY_SAVED', 'GOALKEEPER', 3), ('PENALTY_SAVED', 'DEFENDER', 3), ('PENALTY_SAVED', 'MIDFIELDER', 3), ('PENALTY_SAVED', 'FORWARD', 3),
                          ('PENALTY_MISSED', 'GOALKEEPER', -2), ('PENALTY_MISSED', 'DEFENDER', -2), ('PENALTY_MISSED', 'MIDFIELDER', -2), ('PENALTY_MISSED', 'FORWARD', -2),
                          ('ASSIST', 'GOALKEEPER', 1), ('ASSIST', 'DEFENDER', 1), ('ASSIST', 'MIDFIELDER', 1), ('ASSIST', 'FORWARD', 1),
                          ('OWN_GOAL', 'GOALKEEPER', -3), ('OWN_GOAL', 'DEFENDER', -3), ('OWN_GOAL', 'MIDFIELDER', -3), ('OWN_GOAL', 'FORWARD', -3),
                          ('YELLOW_CARD', 'GOALKEEPER', -1), ('YELLOW_CARD', 'DEFENDER', -1), ('YELLOW_CARD', 'MIDFIELDER', -1), ('YELLOW_CARD', 'FORWARD', -1),
                          ('DOUBLE_YELLOW', 'GOALKEEPER', -3), ('DOUBLE_YELLOW', 'DEFENDER', -3), ('DOUBLE_YELLOW', 'MIDFIELDER', -3), ('DOUBLE_YELLOW', 'FORWARD', -3),
                          ('RED_CARD', 'GOALKEEPER', -5), ('RED_CARD', 'DEFENDER', -5), ('RED_CARD', 'MIDFIELDER', -5), ('RED_CARD', 'FORWARD', -5))
                    AS r (action, position, points);
