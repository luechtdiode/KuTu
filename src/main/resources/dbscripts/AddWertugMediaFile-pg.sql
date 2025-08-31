-- -----------------------------------------------------
-- Table media
-- -----------------------------------------------------
DROP TABLE IF EXISTS media;

CREATE TABLE IF NOT EXISTS media (
  id integer primary key,
  name varchar(200) NOT NULL,
  extension VARCHAR(10) NOT NULL
);

ALTER TABLE wertung            ADD COLUMN media_id integer REFERENCES media (id);
ALTER TABLE athletregistration ADD COLUMN media_id integer REFERENCES media (id);
