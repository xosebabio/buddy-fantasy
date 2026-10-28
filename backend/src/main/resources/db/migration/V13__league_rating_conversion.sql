-- Rating conversion per format (RF-22): points = factor x average + base. Both formats keep theirs, so switching
-- format does not lose the other one. It replaces the single points per unit. With fewer votes than
-- rating_min_votes a player counts as unrated (RF-23).
ALTER TABLE league_settings
  DROP COLUMN rating_points_per_unit,
  ADD COLUMN rating_spades_factor  NUMERIC(6, 2) NOT NULL DEFAULT 4 CHECK (rating_spades_factor > 0),
  ADD COLUMN rating_spades_base    NUMERIC(6, 2) NOT NULL DEFAULT -2,
  ADD COLUMN rating_decimal_factor NUMERIC(6, 2) NOT NULL DEFAULT 1 CHECK (rating_decimal_factor > 0),
  ADD COLUMN rating_decimal_base   NUMERIC(6, 2) NOT NULL DEFAULT 0,
  ADD COLUMN rating_min_votes      INTEGER       NOT NULL DEFAULT 1 CHECK (rating_min_votes >= 1);

-- The defaults only backfill the existing leagues: a new league is seeded from league.defaults.
ALTER TABLE league_settings
  ALTER COLUMN rating_spades_factor DROP DEFAULT,
  ALTER COLUMN rating_spades_base DROP DEFAULT,
  ALTER COLUMN rating_decimal_factor DROP DEFAULT,
  ALTER COLUMN rating_decimal_base DROP DEFAULT,
  ALTER COLUMN rating_min_votes DROP DEFAULT;
