DROP INDEX IF EXISTS xwettkampf_plan_zeitenpk;
DROP VIEW IF EXISTS zeitplan;
DROP VIEW IF EXISTS durchgangeraet;
DROP TABLE IF EXISTS wettkampf_plan_zeiten;
-- -----------------------------------------------------
-- Table wettkampf_plan_zeiten
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS wettkampf_plan_zeiten (
    id integer primary key AUTOINCREMENT,
    wettkampfdisziplin_id integer NOT NULL,
    wettkampf_id integer NOT NULL,
    wechsel long NOT NULL default 30000,
    einturnen long NOT NULL default 30000,
    uebung long NOT NULL default 60000,
    wertung long NOT NULL default 40000,
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
    where exists (select 1 from wertung w where rrr.name in (w.riege, w.riege2));

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
            inner join riege rr on (rr.wettkampf_id = w.wettkampf_id and rr.name in (w.riege, w.riege2) and d.id in (
                select geraet from durchgangeraet dg where dg.wettkampf_id = w.wettkampf_id and dg.durchgang = rr.durchgang
            ))
            inner join wettkampf_plan_zeiten pzz on (pzz.wettkampf_id = w.wettkampf_id and pzz.wettkampfdisziplin_id = w.wettkampfdisziplin_id)
        )
        group by wettkampf_id, durchgang, start
    )
    group by wettkampf_id, durchgang;
