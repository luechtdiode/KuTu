-- -----------------------------------------------------
-- Table scoretemplate
-- -----------------------------------------------------
DROP TABLE IF EXISTS scoretemplate;

CREATE TABLE IF NOT EXISTS scoretemplate (
  id integer primary key AUTOINCREMENT,
  wettkampf_id integer,
  disziplin_id integer,
  wettkampfdisziplin_id integer,
  dFormula varchar(5000) NOT NULL DEFAULT '',
  eFormula varchar(5000) NOT NULL DEFAULT '',
  pFormula varchar(5000) NOT NULL DEFAULT '',
  aggregateFn varchar(100)  NOT NULL DEFAULT '',
  FOREIGN KEY (wettkampf_id) REFERENCES wettkampf (id),
  FOREIGN KEY (disziplin_id) REFERENCES disziplin (id),
  FOREIGN KEY (wettkampfdisziplin_id) REFERENCES wettkampfdisziplin (id)
);

--
ALTER TABLE wertung
  ADD COLUMN variables varchar(5000);

-- set K7 Sprung with 2 jump template
--       "dFormula": "0.0",
--       "eFormula": "$EENote^",
--       "pFormula": "0.0",
--       "aggregateFn": "Max"
--
INSERT INTO scoretemplate (wettkampfdisziplin_id, dFormula, eFormula, pFormula, aggregateFn)
values               (141, "0.0", "$EENote.2^", "0.0", "Max")
;