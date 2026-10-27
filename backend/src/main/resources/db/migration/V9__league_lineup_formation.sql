-- Minimum players per position in a lineup when the formation is enabled (RF-24). No maximum.
ALTER TABLE league_settings
  ADD COLUMN lineup_min_goalkeepers SMALLINT NOT NULL DEFAULT 1 CHECK (lineup_min_goalkeepers >= 0),
  ADD COLUMN lineup_min_defenders   SMALLINT NOT NULL DEFAULT 1 CHECK (lineup_min_defenders >= 0),
  ADD COLUMN lineup_min_midfielders SMALLINT NOT NULL DEFAULT 1 CHECK (lineup_min_midfielders >= 0),
  ADD COLUMN lineup_min_forwards    SMALLINT NOT NULL DEFAULT 1 CHECK (lineup_min_forwards >= 0);

-- Lineups lock at most one week before kickoff, so the lock job only looks at matches in the next 168 h.
ALTER TABLE league_settings
  ADD CONSTRAINT league_settings_lineup_lock_hours_max CHECK (lineup_lock_hours <= 168);
