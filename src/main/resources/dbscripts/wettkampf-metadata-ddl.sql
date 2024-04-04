-- -----------------------------------------------------
-- Table wettkampfmetadata (wettkampf-metadata-ddl.sql)
-- -----------------------------------------------------
DROP TABLE IF EXISTS wettkampfmetadata;

CREATE TABLE IF NOT EXISTS wettkampfmetadata (
  uuid varchar(70) NOT NULL,
  wettkampf_id integer NOT NULL REFERENCES wettkampf (id),
  finish_athletes_cnt int DEFAULT 0,
  finish_clubs_cnt int DEFAULT 0,
  finish_online_athletes_cnt int DEFAULT 0,
  finish_online_clubs_cnt int DEFAULT 0,
  finish_donation_mail varchar(70) DEFAULT NULL,
  finish_donation_asked decimal(10,2) DEFAULT NULL,
  finish_donation_approved decimal(10,2) DEFAULT NULL

  -- emailapproved boolean DEFAULT false,
  -- organisator varchar(100) DEFAULT NULL,
  -- homepage varchar(100) DEFAULT NULL,
  -- wk_adresse varchar(40) DEFAULT NULL,
  -- wk_ort varchar(40) DEFAULT NULL,
  -- wk_land varchar(40) DEFAULT NULL,

  -- finish_teams_cnt int DEFAULT 0,
  -- finish_program_cnt int DEFAULT 0,
);

CREATE UNIQUE INDEX xwettkampfmetadatapk ON wettkampfmetadata (wettkampf_id);
CREATE UNIQUE INDEX xwettkampfmetadatapk2 ON wettkampfmetadata (uuid);

-- -----------------------------------------------------
-- View wkstats
-- -----------------------------------------------------
CREATE VIEW wkstats(wk_uuid, wk_id, wk_titel, finish_athletes_cnt, finish_clubs_cnt, finish_online_athletes_cnt, finish_online_clubs_cnt)
    AS
    select wk.uuid, wk.id, wk.titel
      , count(distinct a.id) as finish_athletes_cnt
      , count(distinct v.id) as finish_clubs_cnt
      , count(distinct ar.id) as finish_online_athletes_cnt
      , count(distinct vr.id) as finish_online_clubs_cnt
    from wettkampf wk
      inner join wertung w on w.wettkampf_id = wk.id
      inner join athlet a on w.athlet_id = a.id
      inner join verein v on v.id = a.verein
      left outer join vereinregistration vr on vr.wettkampf_id = wk.id
      left outer join athletregistration ar on ar.vereinregistration_id = vr.id
    group by wk.uuid, wk.id
;

insert into wettkampfmetadata
  (uuid, wettkampf_id)
  select uuid, id from wettkampf wk where wk.uuid <> ''
  on conflict(wettkampf_id) do nothing
;
