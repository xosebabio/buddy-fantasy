-- Invitation link per league (RF-06). Only the SHA-256 hash of the token is stored.
CREATE TABLE league_invitation
(
  id         UUID                     NOT NULL PRIMARY KEY,
  league_id  UUID                     NOT NULL REFERENCES league_league (id),
  token_hash VARCHAR(64)              NOT NULL UNIQUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked_at TIMESTAMP WITH TIME ZONE
);

-- At most one active invitation per league: regenerating revokes the previous one.
CREATE UNIQUE INDEX league_invitation_active_idx ON league_invitation (league_id) WHERE revoked_at IS NULL;
