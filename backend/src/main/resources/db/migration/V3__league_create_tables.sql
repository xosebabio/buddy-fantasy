CREATE TABLE league_league
(
  id         UUID                     NOT NULL PRIMARY KEY,
  name       VARCHAR(100)             NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE league_membership
(
  id        UUID                     NOT NULL PRIMARY KEY,
  league_id UUID                     NOT NULL REFERENCES league_league (id),
  user_id   UUID                     NOT NULL, -- logical reference to identity_user (no cross-module FK)
  role      VARCHAR(20)              NOT NULL CHECK (role IN ('ADMIN', 'MANAGER')),
  joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT league_membership_unique UNIQUE (league_id, user_id)
);
CREATE INDEX league_membership_user_idx ON league_membership (user_id);
