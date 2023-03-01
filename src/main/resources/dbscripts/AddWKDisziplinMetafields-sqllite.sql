-- Add scale, dnote, min, max and startgeraet fields to wettkampfdisziplin table
-- Defaults for KuTu, KuTuRi
ALTER TABLE wettkampfdisziplin
    ADD COLUMN scale integer NOT NULL DEFAULT 3;

ALTER TABLE wettkampfdisziplin
    ADD COLUMN dnote integer NOT NULL DEFAULT 1;

ALTER TABLE wettkampfdisziplin
    ADD COLUMN min integer NOT NULL DEFAULT 0;

ALTER TABLE wettkampfdisziplin
    ADD COLUMN max integer NOT NULL DEFAULT 30;

ALTER TABLE wettkampfdisziplin
    ADD COLUMN startgeraet integer NOT NULL DEFAULT 1;

-- adjust GeTu
update wettkampfdisziplin
  set scale = 2
  where programm_id in (
    select id from programm where parent_id = 20
  )
  --// Sprung K6/K7)
  and id not in (100, 141)
;
update wettkampfdisziplin
  set dnote = 0,
      max = 30
  where programm_id in (
    select id from programm where parent_id = 20
  )
;
update wettkampfdisziplin
  set startgeraet = 0
  where programm_id in (
    select id from programm where parent_id = 20
  )
  -- Barren
  and disziplin_id = 5
;