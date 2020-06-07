-- -----------------------------------------------------
-- Table riege
-- -----------------------------------------------------
ALTER TABLE riege ADD kind INTEGER DEFAULT 0;

-- -----------------------------------------------------
-- View durchgangeraet
-- -----------------------------------------------------
DROP VIEW durchgangeraet;
CREATE VIEW durchgangeraet(wettkampf_id, durchgang, geraet, geraetname)
    AS
    select distinct wettkampf_id, durchgang, start, d.name
    from riege rrr
        inner join disziplin d on (d.id = start)
    where rrr.kind = 1 or exists (select 1 from wertung w where w.wettkampf_id = rrr.wettkampf_id and rrr.name in (w.riege, w.riege2));

