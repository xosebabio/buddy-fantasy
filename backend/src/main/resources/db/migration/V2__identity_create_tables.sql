CREATE TABLE identity_user
(
  id            UUID                     NOT NULL PRIMARY KEY,
  google_sub    VARCHAR(255)             NOT NULL UNIQUE, -- Google "sub" claim, stable identifier
  email         VARCHAR(320)             NOT NULL,
  display_name  VARCHAR(100)             NOT NULL,
  avatar_url    TEXT,
  created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
  last_login_at TIMESTAMP WITH TIME ZONE NOT NULL
);
