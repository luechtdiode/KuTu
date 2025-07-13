-- -----------------------------------------------------
-- Table scoretemplate
-- -----------------------------------------------------
DROP TABLE IF EXISTS scoretemplate;

CREATE TABLE IF NOT EXISTS scoretemplate (
  id SERIAL primary key,
  wettkampf_id integer,
  disziplin_id integer,
  wettkampfdisziplin_id integer,
  dFormula varchar(5000) NOT NULL DEFAULT '',
  eFormula varchar(5000) NOT NULL DEFAULT '',
  pFormula varchar(5000) NOT NULL DEFAULT '',
  aggregateFn varchar(100),
  FOREIGN KEY (wettkampf_id) REFERENCES wettkampf (id),
  FOREIGN KEY (disziplin_id) REFERENCES disziplin (id),
  FOREIGN KEY (wettkampfdisziplin_id) REFERENCES wettkampfdisziplin (id)
);
CREATE UNIQUE INDEX xwettkampf_scoretemplatespk ON scoretemplate (wettkampf_id, disziplin_id, wettkampfdisziplin_id);

--
ALTER TABLE wertung
  ADD COLUMN variables varchar(5000);
