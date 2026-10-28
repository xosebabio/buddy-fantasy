-- Ratings in scoring (RF-22, RF-23, RF-30): a match is scored once it has its sheet, its locked lineups and the result
-- of its rating window (RatingWindowClosed, RatingsCorrected), in any order. Until the window closes the matchday is
-- not in the standings nor in the histories.
ALTER TABLE scoring_match
  ADD COLUMN ratings_at TIMESTAMP WITH TIME ZONE; -- last RatingWindowClosed or RatingsCorrected applied; NULL = none yet

-- The matches already scored keep their points: they were scored without ratings, as if none had been received.
UPDATE scoring_match SET ratings_at = scored_at WHERE scored_at IS NOT NULL;

-- V12's CHECK (scored_at IS NULL OR (sheet_at IS NOT NULL AND lineups_at IS NOT NULL)), now with the ratings.
ALTER TABLE scoring_match DROP CONSTRAINT scoring_match_check1;
ALTER TABLE scoring_match ADD CONSTRAINT scoring_match_scored_check
  CHECK (scored_at IS NULL OR (sheet_at IS NOT NULL AND lineups_at IS NOT NULL AND ratings_at IS NOT NULL));

-- Copy of the points for ratings of each player in the window's result. Apart from scoring_player_score because it can
-- arrive with players the copy of the sheet does not have yet, or the other way round: a player without a row scores
-- 0 for ratings.
CREATE TABLE scoring_player_rating
(
  match_id      UUID    NOT NULL REFERENCES scoring_match (match_id) ON DELETE CASCADE,
  player_id     UUID    NOT NULL, -- logical reference to squad_player (no cross-module FK)
  rating_points INTEGER NOT NULL,
  PRIMARY KEY (match_id, player_id)
);
