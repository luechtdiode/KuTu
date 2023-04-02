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


alter table programm rename to old_programm;

CREATE TABLE IF NOT EXISTS programm (
  id integer primary key,
  name varchar(100) NOT NULL,
  aggregate INT NOT NULL,
  parent_id integer,
  ord INT NOT NULL DEFAULT 0,
  alter_von int NOT NULL DEFAULT 0,
  alter_bis int NOT NULL DEFAULT 100,
  riegenmode int NOT NULL DEFAULT 1,
  uuid varchar(70) NOT NULL DEFAULT (NewUUID()),
  FOREIGN KEY (parent_id) REFERENCES programm (id)
);
create table if not exists pgmidx (
  id integer primary key,
  uuid varchar(70) NOT NULL
);
insert into pgmidx (id, uuid)
values
 (1,"db387348-c02b-11ed-ba9a-a2890e171f2c")
,(2,"db38741a-c02b-11ed-ba9a-a2890e171f2c")
,(3,"db387474-c02b-11ed-ba9a-a2890e171f2c")
,(11,"db3874c4-c02b-11ed-ba9a-a2890e171f2c")
,(12,"db38750a-c02b-11ed-ba9a-a2890e171f2c")
,(13,"db38755a-c02b-11ed-ba9a-a2890e171f2c")
,(14,"db3875a0-c02b-11ed-ba9a-a2890e171f2c")
,(15,"db3875e6-c02b-11ed-ba9a-a2890e171f2c")
,(16,"db387636-c02b-11ed-ba9a-a2890e171f2c")
,(17,"db38767c-c02b-11ed-ba9a-a2890e171f2c")
,(18,"db3876c2-c02b-11ed-ba9a-a2890e171f2c")
,(19,"db387708-c02b-11ed-ba9a-a2890e171f2c")
,(27,"db387758-c02b-11ed-ba9a-a2890e171f2c")
,(31,"db38779e-c02b-11ed-ba9a-a2890e171f2c")
,(32,"db3877e4-c02b-11ed-ba9a-a2890e171f2c")
,(33,"db387834-c02b-11ed-ba9a-a2890e171f2c")
,(34,"db38787a-c02b-11ed-ba9a-a2890e171f2c")
,(35,"db3878c0-c02b-11ed-ba9a-a2890e171f2c")
,(36,"db387906-c02b-11ed-ba9a-a2890e171f2c")
,(37,"db38794c-c02b-11ed-ba9a-a2890e171f2c")
,(38,"db38799c-c02b-11ed-ba9a-a2890e171f2c")
,(39,"db3879e2-c02b-11ed-ba9a-a2890e171f2c")
,(40,"db387a28-c02b-11ed-ba9a-a2890e171f2c")
,(20,"db387a6e-c02b-11ed-ba9a-a2890e171f2c")
,(21,"db387abe-c02b-11ed-ba9a-a2890e171f2c")
,(22,"db387b04-c02b-11ed-ba9a-a2890e171f2c")
,(23,"db387b4a-c02b-11ed-ba9a-a2890e171f2c")
,(24,"db387b90-c02b-11ed-ba9a-a2890e171f2c")
,(25,"db387be0-c02b-11ed-ba9a-a2890e171f2c")
,(26,"db387c26-c02b-11ed-ba9a-a2890e171f2c")
,(41,"db387c6c-c02b-11ed-ba9a-a2890e171f2c")
,(42,"db387cb2-c02b-11ed-ba9a-a2890e171f2c")
,(43,"db387cf8-c02b-11ed-ba9a-a2890e171f2c")
;
-- insert given uuid's of existing pgm's
insert into programm( id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid ) select op.*, t.uuid from old_programm op
 inner join pgmidx t on (t.id = op.id)
;
CREATE UNIQUE INDEX xprogrammuuidpk ON programm (uuid);

drop table pgmidx;
drop table old_programm;

update programm
   set riegenmode=2
 where id=1
    or parent_id=1
;
-- fix age-limitations K1-K4 inofficial, for GeTu BLTV
-- KD official STV
UPDATE programm
  set alter_von=22
  where id=42;
-- KH official STV
UPDATE programm
  set alter_von=28
  where id=43;
