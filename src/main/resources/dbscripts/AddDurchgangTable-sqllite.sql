DROP INDEX IF EXISTS xdurchgangpk;
DROP VIEW IF EXISTS durchgang_zeitplan;
DROP TABLE IF EXISTS durchgang;
-- -----------------------------------------------------
-- Table durchgang
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS durchgang (
    id integer PRIMARY KEY AUTOINCREMENT,
    wettkampf_id integer NOT NULL REFERENCES wettkampf (id),
    ordinal int NOT NULL DEFAULT 0,
    name varchar(255) NOT NULL,
    title varchar(255) NOT NULL,
    durchgangType integer NOT NULL DEFAULT 1,
    planStartOffset long NOT NULL DEFAULT 0,
    effectiveStartTime TIMESTAMP,
    effectiveEndTime TIMESTAMP
);

CREATE UNIQUE INDEX xdurchgangpk ON durchgang (wettkampf_id, name);

-- -----------------------------------------------------
-- View durchgang_zeitplan
-- -----------------------------------------------------
CREATE VIEW IF NOT EXISTS durchgang_zeitplan(
        wettkampf_id,
        title,
        ordinal,
        plan_einturnen,
        plan_uebung,
        plan_wertung,
        plan_total
    ) AS
    SELECT
        dg.wettkampf_id,
        dg.title,
        dg.ordinal,
        max(einturnen) as plan_einturnen,
        max(uebung) as plan_uebung,
        max(wertung) as plan_wertung,
        max(einturnen) + max(uebung) + max(wertung) as plan_total
    FROM durchgang dg
        LEFT OUTER JOIN zeitplan zp ON (zp.wettkampf_id = dg.wettkampf_id and zp.durchgang = dg.name)
    GROUP BY dg.wettkampf_id, dg.title, dg.ordinal
    ORDER BY dg.wettkampf_id, dg.ordinal, dg.title;
