DROP INDEX IF EXISTS xwettkampf_plan_zeitenpk;
DROP VIEW IF EXISTS zeitplan;
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
-- View zeitplan
-- -----------------------------------------------------
CREATE VIEW zeitplan(wettkampf_id, durchgang, einturnen, uebung, wertung)
    AS
    select wettkampf_id, durchgang, max(et) / 60000 as einturnen, max(ueb) / 60000 as uebung, max(wert) / 60000 as wertung
    from (
        select wettkampf_id, durchgang, name, start, sum(einturnen) as et, sum(uebung) as ueb, sum(wertung) as wert
        from (
            select distinct rr.wettkampf_id, rr.start, rr.durchgang, rr.name, w.athlet_id, w.id, pzz.einturnen, pzz.uebung, pzz.wertung
            from wertung w
            inner join riege rr on (rr.wettkampf_id = w.wettkampf_id and rr.name in (w.riege, w.riege2))
            inner join wettkampf_plan_zeiten pzz on (pzz.wettkampf_id = w.wettkampf_id and pzz.wettkampfdisziplin_id = w.wettkampfdisziplin_id)
        ) AS times_by_wertungen
        group by wettkampf_id, durchgang, name, start
    ) AS times_by_geraet
    group by wettkampf_id, durchgang;
