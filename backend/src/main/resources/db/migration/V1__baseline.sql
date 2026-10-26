-- Spring Modulith event publication registry (D14)
CREATE TABLE event_publication
(
  id               UUID                     NOT NULL,
  listener_id      TEXT                     NOT NULL,
  event_type       TEXT                     NOT NULL,
  serialized_event TEXT                     NOT NULL,
  publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date  TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);

-- ShedLock: distributed lock for @Scheduled jobs
CREATE TABLE shedlock
(
  name       VARCHAR(64)  NOT NULL,
  lock_until TIMESTAMP(3) NOT NULL,
  locked_at  TIMESTAMP(3) NOT NULL,
  locked_by  VARCHAR(255) NOT NULL,
  PRIMARY KEY (name)
);
