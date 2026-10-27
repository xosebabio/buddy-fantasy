-- Business limits (name lengths, max goals and stats, shirt numbers, matchday minimum, lineup lock hours) are now
-- configuration (league.limits, squad.limits, match.limits) checked by the domain on every write. The database
-- keeps only structural rules: non-negative values and the size of each column type.

-- league
ALTER TABLE league_settings DROP CONSTRAINT league_settings_lineup_lock_hours_max;
ALTER TABLE league_league ALTER COLUMN name TYPE VARCHAR;

-- squad
ALTER TABLE squad_player ALTER COLUMN name TYPE VARCHAR;
ALTER TABLE squad_player DROP CONSTRAINT squad_player_shirt_number_check;
ALTER TABLE squad_player ADD CONSTRAINT squad_player_shirt_number_check CHECK (shirt_number >= 0);

-- match
ALTER TABLE match_match ALTER COLUMN opponent TYPE VARCHAR;
ALTER TABLE match_match DROP CONSTRAINT match_match_matchday_check;
ALTER TABLE match_match ADD CONSTRAINT match_match_matchday_check CHECK (matchday >= 0);

ALTER TABLE match_player_performance
  DROP CONSTRAINT match_player_performance_goals_check,
  DROP CONSTRAINT match_player_performance_penalty_goals_check,
  DROP CONSTRAINT match_player_performance_penalties_saved_check,
  DROP CONSTRAINT match_player_performance_penalties_missed_check,
  DROP CONSTRAINT match_player_performance_assists_check,
  DROP CONSTRAINT match_player_performance_own_goals_check,
  ADD CONSTRAINT match_player_performance_goals_check CHECK (goals >= 0),
  ADD CONSTRAINT match_player_performance_penalty_goals_check CHECK (penalty_goals >= 0),
  ADD CONSTRAINT match_player_performance_penalties_saved_check CHECK (penalties_saved >= 0),
  ADD CONSTRAINT match_player_performance_penalties_missed_check CHECK (penalties_missed >= 0),
  ADD CONSTRAINT match_player_performance_assists_check CHECK (assists >= 0),
  ADD CONSTRAINT match_player_performance_own_goals_check CHECK (own_goals >= 0);
