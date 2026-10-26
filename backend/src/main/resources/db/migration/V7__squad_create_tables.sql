-- Players of the league's real team (RF-09, RF-10). Retiring is a soft delete: past matches keep the player.
CREATE TABLE squad_player
(
  id            UUID                     NOT NULL PRIMARY KEY,
  league_id     UUID                     NOT NULL, -- logical reference to league_league (no cross-module FK)
  name          VARCHAR(100)             NOT NULL,
  shirt_number  SMALLINT CHECK (shirt_number BETWEEN 0 AND 99), -- NULL = no shirt number
  position      VARCHAR(20)              NOT NULL CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD')),
  initial_value NUMERIC(10, 2)           NOT NULL CHECK (initial_value > 0), -- millions
  photo_key     VARCHAR(200), -- S3 object key, not a URL
  created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
  retired_at    TIMESTAMP WITH TIME ZONE -- NULL = active
);
CREATE INDEX squad_player_league_idx ON squad_player (league_id);

-- Shirt numbers are unique among active players only: retiring a player frees the number.
CREATE UNIQUE INDEX squad_player_active_shirt_number_idx ON squad_player (league_id, shirt_number)
  WHERE retired_at IS NULL AND shirt_number IS NOT NULL;
