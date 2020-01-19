DROP INDEX IF EXISTS xwettkampf_plan_zeitenpk;
DROP VIEW IF EXISTS zeitplan CASCADE;
DROP VIEW IF EXISTS durchgangeraet CASCADE;
DROP TABLE IF EXISTS wettkampf_plan_zeiten;
-- -----------------------------------------------------
-- Table wettkampf_plan_zeiten
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS wettkampf_plan_zeiten (
    id SERIAL primary key,
    wettkampfdisziplin_id integer NOT NULL,
    wettkampf_id integer NOT NULL,
    wechsel integer NOT NULL default 30000,
    einturnen integer NOT NULL default 30000,
    uebung integer NOT NULL default 30000,
    wertung integer NOT NULL default 30000,
    FOREIGN KEY (wettkampfdisziplin_id) REFERENCES wettkampfdisziplin (id),
    FOREIGN KEY (wettkampf_id) REFERENCES wettkampf (id)
);
CREATE UNIQUE INDEX xwettkampf_plan_zeitenpk ON wettkampf_plan_zeiten (wettkampf_id, wettkampfdisziplin_id);

-- -----------------------------------------------------
-- View durchgangeraet
-- -----------------------------------------------------
CREATE VIEW durchgangeraet(wettkampf_id, durchgang, geraet, geraetname)
    AS
    select distinct wettkampf_id, durchgang, start, d.name
    from riege rrr
        inner join disziplin d on (d.id = start)
    where exists (select 1 from wertung w where w.wettkampf_id = rrr.wettkampf_id and rrr.name in (w.riege, w.riege2));

-- -----------------------------------------------------
-- View zeitplan
-- -----------------------------------------------------
CREATE VIEW zeitplan(wettkampf_id, durchgang, einturnen, geraet, total)
    AS
    select wettkampf_id, durchgang, max(et)/count(start) as einturnen, (max(ueb)+max(wert))/count(start) as geraet, (max(wechsel) + max(et) + max(ueb) + max(wert)) as total
    from (
        select wettkampf_id, durchgang, start, sum(wechsel) as wechsel, sum(einturnen) as et, sum(uebung) as ueb, sum(wertung) as wert
        from (
            select distinct rr.wettkampf_id, rr.start, rr.durchgang, w.athlet_id, w.id, pzz.wechsel, pzz.einturnen, pzz.uebung, pzz.wertung
            from wertung w
            inner join wettkampfdisziplin wd on (wd.id = w.wettkampfdisziplin_id)
            inner join disziplin d on (d.id = wd.disziplin_id)
            inner join riege rr on (rr.wettkampf_id = w.wettkampf_id and rr.name in (w.riege, w.riege2) and exists (
                select geraet from durchgangeraet dg where dg.wettkampf_id = w.wettkampf_id and dg.geraet = d.id and dg.durchgang = rr.durchgang
            ))
            inner join wettkampf_plan_zeiten pzz on (pzz.wettkampf_id = w.wettkampf_id and pzz.wettkampfdisziplin_id = w.wettkampfdisziplin_id)
        ) AS times_by_wertungen
        group by wettkampf_id, durchgang, start
    ) AS times_by_geraet
    group by wettkampf_id, durchgang;
