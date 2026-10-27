-- Matches of the league's real team (RF-12). 1 matchday = 1 match; chronological order is kickoff_at, not matchday.
CREATE TABLE match_match
(
  id            UUID                     NOT NULL PRIMARY KEY,
  league_id     UUID                     NOT NULL, -- logical reference to league_league (no cross-module FK)
  matchday      SMALLINT                 NOT NULL CHECK (matchday >= 1),
  opponent      VARCHAR(100)             NOT NULL,
  venue         VARCHAR(10)              NOT NULL CHECK (venue IN ('HOME', 'AWAY')),
  kickoff_at    TIMESTAMP WITH TIME ZONE NOT NULL,
  status        VARCHAR(10)              NOT NULL CHECK (status IN ('SCHEDULED', 'CLOSED')),
  goals_for     SMALLINT CHECK (goals_for >= 0), -- NULL = no result yet
  goals_against SMALLINT CHECK (goals_against >= 0),
  created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
  closed_at     TIMESTAMP WITH TIME ZONE,
  CHECK ((goals_for IS NULL) = (goals_against IS NULL)),
  CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL))
);
CREATE UNIQUE INDEX match_match_league_matchday_idx ON match_match (league_id, matchday);
-- The team never plays two matches at once: a total order for "previous matchday" (RF-28).
CREATE UNIQUE INDEX match_match_league_kickoff_idx ON match_match (league_id, kickoff_at);

-- Who played and their stats (RF-13, RF-14). One row per player and match; replaced as a whole with the sheet.
CREATE TABLE match_player_performance
(
  match_id         UUID        NOT NULL REFERENCES match_match (id) ON DELETE CASCADE,
  player_id        UUID        NOT NULL, -- logical reference to squad_player (no cross-module FK)
  -- Snapshot of the player's position when added to the sheet: later squad edits do not change past matches.
  position         VARCHAR(20) NOT NULL CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD')),
  goals            SMALLINT    NOT NULL DEFAULT 0 CHECK (goals BETWEEN 0 AND 99),
  penalty_goals    SMALLINT    NOT NULL DEFAULT 0 CHECK (penalty_goals BETWEEN 0 AND 99),
  penalties_saved  SMALLINT    NOT NULL DEFAULT 0 CHECK (penalties_saved BETWEEN 0 AND 99),
  penalties_missed SMALLINT    NOT NULL DEFAULT 0 CHECK (penalties_missed BETWEEN 0 AND 99),
  assists          SMALLINT    NOT NULL DEFAULT 0 CHECK (assists BETWEEN 0 AND 99),
  own_goals        SMALLINT    NOT NULL DEFAULT 0 CHECK (own_goals BETWEEN 0 AND 99),
  yellow_card      BOOLEAN     NOT NULL DEFAULT FALSE,
  double_yellow    BOOLEAN     NOT NULL DEFAULT FALSE,
  red_card         BOOLEAN     NOT NULL DEFAULT FALSE,
  PRIMARY KEY (match_id, player_id),
  -- A double yellow is the only sending-off and already includes the yellow (-3 in total).
  CHECK (NOT (double_yellow AND (yellow_card OR red_card)))
);

-- Insert-only log of the admin's changes to matches (RNF-04). No FK: entries outlive a deleted match.
CREATE TABLE match_audit_entry
(
  id            UUID                     NOT NULL PRIMARY KEY,
  league_id     UUID                     NOT NULL,
  match_id      UUID                     NOT NULL,
  actor_user_id UUID                     NOT NULL,
  action        VARCHAR(30)              NOT NULL CHECK (action IN ('SCHEDULED', 'DETAILS_UPDATED', 'RESCHEDULED',
                                                                    'SHEET_RECORDED', 'CLOSED', 'CORRECTED', 'DELETED')),
  occurred_at   TIMESTAMP WITH TIME ZONE NOT NULL,
  before        JSONB, -- NULL when the match is scheduled
  after         JSONB  -- NULL when the match is deleted
);
CREATE INDEX match_audit_entry_match_idx ON match_audit_entry (match_id, occurred_at);
