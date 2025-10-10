-- -----------------------------------------------------
-- Table media
-- -----------------------------------------------------
DROP TABLE IF EXISTS media;

CREATE TABLE IF NOT EXISTS media (
  id varchar(70) primary key,
  name varchar(200) NOT NULL,
  extension VARCHAR(10) NOT NULL,
  stage integer NOT NULL DEFAULT 0,
  metadata VARCHAR(400) NOT NULL DEFAULT '',
  md5 VARCHAR(32) NOT NULL DEFAULT '',
  stamp timestamp
);

CREATE OR REPLACE FUNCTION set_timestamp() RETURNS TRIGGER AS $$ BEGIN UPDATE media SET stamp=CURRENT_TIMESTAMP WHERE id=NEW.id; RETURN new; END; $$ language plpgsql;

CREATE TRIGGER media_insert
  AFTER INSERT ON media
  FOR EACH ROW
  EXECUTE FUNCTION set_timestamp();

ALTER TABLE wertung            ADD COLUMN media_id varchar(70) REFERENCES media (id);
ALTER TABLE athletregistration ADD COLUMN media_id varchar(70) REFERENCES media (id);

CREATE INDEX xwertung_media ON wertung (media_id);
CREATE INDEX xathletregistration_media ON athletregistration (media_id);