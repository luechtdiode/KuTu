-- Add scale, dnote, min, max and startgeraet fields to wettkampfdisziplin table
-- Defaults for KuTu, KuTuRi
ALTER TABLE IF EXISTS wettkampfdisziplin
    ADD COLUMN scale integer NOT NULL DEFAULT 3;

ALTER TABLE IF EXISTS wettkampfdisziplin
    ADD COLUMN dnote integer NOT NULL DEFAULT 1;

ALTER TABLE IF EXISTS wettkampfdisziplin
    ADD COLUMN min integer NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS wettkampfdisziplin
    ADD COLUMN max integer NOT NULL DEFAULT 30;

ALTER TABLE IF EXISTS wettkampfdisziplin
    ADD COLUMN startgeraet integer NOT NULL DEFAULT 1;

-- adjust GeTu
update wettkampfdisziplin
  set scale = 2
  where programm_id in (
    select id from programm where parent_id = 20
  )
-- Sprung K6/K7)
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
-- KD official STV
UPDATE programm
  set alter_von=22
  where id=42
;
-- KH official STV
UPDATE programm
  set alter_von=28
  where id=43
;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
ALTER TABLE IF EXISTS programm
    ADD COLUMN uuid varchar(70);

-- insert given uuid's of existing pgm's
create table if not exists pgmidx (
  id integer primary key,
  uuid varchar(70) NOT NULL
);

insert into pgmidx (id, uuid) values
 (1,'db387348-c02b-11ed-ba9a-a2890e171f2c')
,(2,'db38741a-c02b-11ed-ba9a-a2890e171f2c')
,(3,'db387474-c02b-11ed-ba9a-a2890e171f2c')
,(11,'db3874c4-c02b-11ed-ba9a-a2890e171f2c')
,(12,'db38750a-c02b-11ed-ba9a-a2890e171f2c')
,(13,'db38755a-c02b-11ed-ba9a-a2890e171f2c')
,(14,'db3875a0-c02b-11ed-ba9a-a2890e171f2c')
,(15,'db3875e6-c02b-11ed-ba9a-a2890e171f2c')
,(16,'db387636-c02b-11ed-ba9a-a2890e171f2c')
,(17,'db38767c-c02b-11ed-ba9a-a2890e171f2c')
,(18,'db3876c2-c02b-11ed-ba9a-a2890e171f2c')
,(19,'db387708-c02b-11ed-ba9a-a2890e171f2c')
,(27,'db387758-c02b-11ed-ba9a-a2890e171f2c')
,(31,'db38779e-c02b-11ed-ba9a-a2890e171f2c')
,(32,'db3877e4-c02b-11ed-ba9a-a2890e171f2c')
,(33,'db387834-c02b-11ed-ba9a-a2890e171f2c')
,(34,'db38787a-c02b-11ed-ba9a-a2890e171f2c')
,(35,'db3878c0-c02b-11ed-ba9a-a2890e171f2c')
,(36,'db387906-c02b-11ed-ba9a-a2890e171f2c')
,(37,'db38794c-c02b-11ed-ba9a-a2890e171f2c')
,(38,'db38799c-c02b-11ed-ba9a-a2890e171f2c')
,(39,'db3879e2-c02b-11ed-ba9a-a2890e171f2c')
,(40,'db387a28-c02b-11ed-ba9a-a2890e171f2c')
,(20,'db387a6e-c02b-11ed-ba9a-a2890e171f2c')
,(21,'db387abe-c02b-11ed-ba9a-a2890e171f2c')
,(22,'db387b04-c02b-11ed-ba9a-a2890e171f2c')
,(23,'db387b4a-c02b-11ed-ba9a-a2890e171f2c')
,(24,'db387b90-c02b-11ed-ba9a-a2890e171f2c')
,(25,'db387be0-c02b-11ed-ba9a-a2890e171f2c')
,(26,'db387c26-c02b-11ed-ba9a-a2890e171f2c')
,(41,'db387c6c-c02b-11ed-ba9a-a2890e171f2c')
,(42,'db387cb2-c02b-11ed-ba9a-a2890e171f2c')
,(43,'db387cf8-c02b-11ed-ba9a-a2890e171f2c')
;
update programm p
  set uuid = (select uuid from pgmidx i where i.id = p.id);

ALTER TABLE IF EXISTS programm
    ALTER COLUMN uuid SET DEFAULT uuid_generate_v1();
CREATE UNIQUE INDEX xprogrammuuidpk ON programm (uuid);

drop table pgmidx;

ALTER TABLE IF EXISTS programm
    ADD COLUMN riegenmode integer NOT NULL DEFAULT 1;

update programm
   set riegenmode=2
 where id=1
    or parent_id=1
;

-- Test Programm-Extensions
-- KuTu DTL Kür & Pflicht
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into disziplin (id, name) values (3, 'Ring') on conflict (id) do nothing;
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into disziplin (id, name) values (6, 'Reck') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values
(184, 'KuTu DTL Kür & Pflicht', 0, null, 184, 0, 100, '29ee216c-f1bf-4940-9b2f-a98b6af0ef73', 1)
,(185, 'Kür', 1, 184, 185, 0, 100, '9868786d-29fd-4560-8a14-a32a5dd56c7a', 1)
,(186, 'WK I Kür', 1, 185, 186, 0, 100, 'a731a36f-2190-403a-8dff-5009b862d6b2', 1)
,(187, 'WK II LK1', 1, 185, 187, 0, 100, '1aa8711c-5bf3-45bd-98cf-8f143fe57f94', 1)
,(188, 'WK III LK1', 1, 185, 188, 16, 17, '0a685ee1-e4bd-450a-b387-5470d4424e13', 1)
,(189, 'WK IV LK2', 1, 185, 189, 14, 15, '6517a841-fc1e-49fa-8d45-9c90cad2a739', 1)
,(190, 'Pflicht', 1, 184, 190, 0, 100, 'df6de257-03e0-4b61-8855-e7f9c4b416b0', 1)
,(191, 'WK V Jug', 1, 190, 191, 14, 18, '1f29b75e-6800-4eab-9350-1d5029bafa69', 1)
,(192, 'WK VI Schüler A', 1, 190, 192, 12, 13, 'd7b983af-cbac-4ffc-bbe1-90255a2c828c', 1)
,(193, 'WK VII Schüler B', 1, 190, 193, 10, 11, 'acddda1d-3561-4bf8-9c3e-c8d6caaa5771', 1)
,(194, 'WK VIII Schüler C', 1, 190, 194, 8, 9, '7f0921ec-8e35-4f00-b9d4-9a9e7fccc3d8', 1)
,(195, 'WK IX Schüler D', 1, 190, 195, 0, 7, '67dd95d0-5b48-4f82-b109-8c564754b577', 1);
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values
(847, 186, 1, '', '', 1.0, 1, 0, 0, 3, 1, 0, 30, 1)
,(848, 186, 3, '', '', 1.0, 1, 0, 1, 3, 1, 0, 30, 1)
,(849, 186, 4, '', '', 1.0, 1, 0, 2, 3, 1, 0, 30, 1)
,(850, 186, 5, '', '', 1.0, 1, 0, 3, 3, 1, 0, 30, 1)
,(851, 186, 6, '', '', 1.0, 1, 0, 4, 3, 1, 0, 30, 1)
,(852, 187, 1, '', '', 1.0, 1, 0, 5, 3, 1, 0, 30, 1)
,(853, 187, 3, '', '', 1.0, 1, 0, 6, 3, 1, 0, 30, 1)
,(854, 187, 4, '', '', 1.0, 1, 0, 7, 3, 1, 0, 30, 1)
,(855, 187, 5, '', '', 1.0, 1, 0, 8, 3, 1, 0, 30, 1)
,(856, 187, 6, '', '', 1.0, 1, 0, 9, 3, 1, 0, 30, 1)
,(857, 188, 1, '', '', 1.0, 1, 0, 10, 3, 1, 0, 30, 1)
,(858, 188, 3, '', '', 1.0, 1, 0, 11, 3, 1, 0, 30, 1)
,(859, 188, 4, '', '', 1.0, 1, 0, 12, 3, 1, 0, 30, 1)
,(860, 188, 5, '', '', 1.0, 1, 0, 13, 3, 1, 0, 30, 1)
,(861, 188, 6, '', '', 1.0, 1, 0, 14, 3, 1, 0, 30, 1)
,(862, 189, 1, '', '', 1.0, 1, 0, 15, 3, 1, 0, 30, 1)
,(863, 189, 3, '', '', 1.0, 1, 0, 16, 3, 1, 0, 30, 1)
,(864, 189, 4, '', '', 1.0, 1, 0, 17, 3, 1, 0, 30, 1)
,(865, 189, 5, '', '', 1.0, 1, 0, 18, 3, 1, 0, 30, 1)
,(866, 189, 6, '', '', 1.0, 1, 0, 19, 3, 1, 0, 30, 1)
,(867, 191, 1, '', '', 1.0, 1, 0, 20, 3, 1, 0, 30, 1)
,(868, 191, 3, '', '', 1.0, 1, 0, 21, 3, 1, 0, 30, 1)
,(869, 191, 4, '', '', 1.0, 1, 0, 22, 3, 1, 0, 30, 1)
,(870, 191, 5, '', '', 1.0, 1, 0, 23, 3, 1, 0, 30, 1)
,(871, 191, 6, '', '', 1.0, 1, 0, 24, 3, 1, 0, 30, 1)
,(872, 192, 1, '', '', 1.0, 1, 0, 25, 3, 1, 0, 30, 1)
,(873, 192, 3, '', '', 1.0, 1, 0, 26, 3, 1, 0, 30, 1)
,(874, 192, 4, '', '', 1.0, 1, 0, 27, 3, 1, 0, 30, 1)
,(875, 192, 5, '', '', 1.0, 1, 0, 28, 3, 1, 0, 30, 1)
,(876, 192, 6, '', '', 1.0, 1, 0, 29, 3, 1, 0, 30, 1)
,(877, 193, 1, '', '', 1.0, 1, 0, 30, 3, 1, 0, 30, 1)
,(878, 193, 3, '', '', 1.0, 1, 0, 31, 3, 1, 0, 30, 1)
,(879, 193, 4, '', '', 1.0, 1, 0, 32, 3, 1, 0, 30, 1)
,(880, 193, 5, '', '', 1.0, 1, 0, 33, 3, 1, 0, 30, 1)
,(881, 193, 6, '', '', 1.0, 1, 0, 34, 3, 1, 0, 30, 1)
,(882, 194, 1, '', '', 1.0, 1, 0, 35, 3, 1, 0, 30, 1)
,(883, 194, 3, '', '', 1.0, 1, 0, 36, 3, 1, 0, 30, 1)
,(884, 194, 4, '', '', 1.0, 1, 0, 37, 3, 1, 0, 30, 1)
,(885, 194, 5, '', '', 1.0, 1, 0, 38, 3, 1, 0, 30, 1)
,(886, 194, 6, '', '', 1.0, 1, 0, 39, 3, 1, 0, 30, 1)
,(887, 195, 1, '', '', 1.0, 1, 0, 40, 3, 1, 0, 30, 1)
,(888, 195, 3, '', '', 1.0, 1, 0, 41, 3, 1, 0, 30, 1)
,(889, 195, 4, '', '', 1.0, 1, 0, 42, 3, 1, 0, 30, 1)
,(890, 195, 5, '', '', 1.0, 1, 0, 43, 3, 1, 0, 30, 1)
,(891, 195, 6, '', '', 1.0, 1, 0, 44, 3, 1, 0, 30, 1);

-- KuTuRi DTL Kür & Pflicht
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into disziplin (id, name) values (28, 'Balken') on conflict (id) do nothing;
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values
(196, 'KuTuRi DTL Kür & Pflicht', 0, null, 196, 0, 100, 'b66dac1b-e2cf-43b5-a919-0d94aa42680c', 1)
,(197, 'Kür', 1, 196, 197, 0, 100, 'b371f1b4-ff84-4a65-b536-9ff6e0479a6d', 1)
,(198, 'WK I Kür', 1, 197, 198, 0, 100, '932da381-63ea-4166-9bb5-04d7d684d105', 1)
,(199, 'WK II LK1', 1, 197, 199, 0, 100, '146a17d5-7ab4-4086-81f1-db3329b07e2a', 1)
,(200, 'WK III LK1', 1, 197, 200, 16, 17, 'b70b2f6d-1321-4666-bac8-5807a15786eb', 1)
,(201, 'WK IV LK2', 1, 197, 201, 14, 15, '28efc451-d762-42ea-9618-b4c0f23cbe9b', 1)
,(202, 'Pflicht', 1, 196, 202, 0, 100, '04d249b9-5c2c-4a97-a6bc-ee86062d6cac', 1)
,(203, 'WK V Jug', 1, 202, 203, 14, 18, 'd0c91f7c-fe42-466a-a7c0-c3bec17133fe', 1)
,(204, 'WK VI Schüler A', 1, 202, 204, 12, 13, '45584b7f-498c-42a8-b6bb-c06525cac332', 1)
,(205, 'WK VII Schüler B', 1, 202, 205, 10, 11, '863da537-b5d0-4259-ae62-b358f70c0ff4', 1)
,(206, 'WK VIII Schüler C', 1, 202, 206, 8, 9, '6567925e-2335-421c-b677-d426adb258e3', 1)
,(207, 'WK IX Schüler D', 1, 202, 207, 0, 7, 'e5224fa7-15c8-4840-8c9c-f2db160e1147', 1);
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values
(892, 198, 4, '', '', 1.0, 0, 1, 0, 3, 1, 0, 30, 1)
,(893, 198, 5, '', '', 1.0, 0, 1, 1, 3, 1, 0, 30, 1)
,(894, 198, 28, '', '', 1.0, 0, 1, 2, 3, 1, 0, 30, 1)
,(895, 198, 1, '', '', 1.0, 0, 1, 3, 3, 1, 0, 30, 1)
,(896, 199, 4, '', '', 1.0, 0, 1, 4, 3, 1, 0, 30, 1)
,(897, 199, 5, '', '', 1.0, 0, 1, 5, 3, 1, 0, 30, 1)
,(898, 199, 28, '', '', 1.0, 0, 1, 6, 3, 1, 0, 30, 1)
,(899, 199, 1, '', '', 1.0, 0, 1, 7, 3, 1, 0, 30, 1)
,(900, 200, 4, '', '', 1.0, 0, 1, 8, 3, 1, 0, 30, 1)
,(901, 200, 5, '', '', 1.0, 0, 1, 9, 3, 1, 0, 30, 1)
,(902, 200, 28, '', '', 1.0, 0, 1, 10, 3, 1, 0, 30, 1)
,(903, 200, 1, '', '', 1.0, 0, 1, 11, 3, 1, 0, 30, 1)
,(904, 201, 4, '', '', 1.0, 0, 1, 12, 3, 1, 0, 30, 1)
,(905, 201, 5, '', '', 1.0, 0, 1, 13, 3, 1, 0, 30, 1)
,(906, 201, 28, '', '', 1.0, 0, 1, 14, 3, 1, 0, 30, 1)
,(907, 201, 1, '', '', 1.0, 0, 1, 15, 3, 1, 0, 30, 1)
,(908, 203, 4, '', '', 1.0, 0, 1, 16, 3, 1, 0, 30, 1)
,(909, 203, 5, '', '', 1.0, 0, 1, 17, 3, 1, 0, 30, 1)
,(910, 203, 28, '', '', 1.0, 0, 1, 18, 3, 1, 0, 30, 1)
,(911, 203, 1, '', '', 1.0, 0, 1, 19, 3, 1, 0, 30, 1)
,(912, 204, 4, '', '', 1.0, 0, 1, 20, 3, 1, 0, 30, 1)
,(913, 204, 5, '', '', 1.0, 0, 1, 21, 3, 1, 0, 30, 1)
,(914, 204, 28, '', '', 1.0, 0, 1, 22, 3, 1, 0, 30, 1)
,(915, 204, 1, '', '', 1.0, 0, 1, 23, 3, 1, 0, 30, 1)
,(916, 205, 4, '', '', 1.0, 0, 1, 24, 3, 1, 0, 30, 1)
,(917, 205, 5, '', '', 1.0, 0, 1, 25, 3, 1, 0, 30, 1)
,(918, 205, 28, '', '', 1.0, 0, 1, 26, 3, 1, 0, 30, 1)
,(919, 205, 1, '', '', 1.0, 0, 1, 27, 3, 1, 0, 30, 1)
,(920, 206, 4, '', '', 1.0, 0, 1, 28, 3, 1, 0, 30, 1)
,(921, 206, 5, '', '', 1.0, 0, 1, 29, 3, 1, 0, 30, 1)
,(922, 206, 28, '', '', 1.0, 0, 1, 30, 3, 1, 0, 30, 1)
,(923, 206, 1, '', '', 1.0, 0, 1, 31, 3, 1, 0, 30, 1)
,(924, 207, 4, '', '', 1.0, 0, 1, 32, 3, 1, 0, 30, 1)
,(925, 207, 5, '', '', 1.0, 0, 1, 33, 3, 1, 0, 30, 1)
,(926, 207, 28, '', '', 1.0, 0, 1, 34, 3, 1, 0, 30, 1)
,(927, 207, 1, '', '', 1.0, 0, 1, 35, 3, 1, 0, 30, 1);

-- Turn10-verein
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into disziplin (id, name) values (28, 'Balken') on conflict (id) do nothing;
insert into disziplin (id, name) values (30, 'Minitramp') on conflict (id) do nothing;
insert into disziplin (id, name) values (6, 'Reck') on conflict (id) do nothing;
insert into disziplin (id, name) values (27, 'Stufenbarren') on conflict (id) do nothing;
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into disziplin (id, name) values (32, 'Ringe') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values
(208, 'Turn10-Verein', 0, null, 208, 0, 100, 'b2d95501-52d2-4070-ab8c-27406cddb8fb', 2)
,(209, 'BS', 0, 208, 209, 0, 100, '812702c4-39b5-47c1-9c32-30243e7433a7', 2)
,(210, 'OS', 0, 208, 210, 0, 100, '6ea03ad7-0a10-4d34-b7b2-730f6325ef00', 2);
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values
(928, 209, 1, '', '', 1.0, 1, 1, 0, 3, 1, 0, 30, 1)
,(929, 209, 5, '', '', 1.0, 1, 0, 1, 3, 1, 0, 30, 1)
,(930, 209, 28, '', '', 1.0, 0, 1, 2, 3, 1, 0, 30, 1)
,(931, 209, 30, '', '', 1.0, 1, 1, 3, 3, 1, 0, 30, 1)
,(932, 209, 6, '', '', 1.0, 1, 0, 4, 3, 1, 0, 30, 1)
,(933, 209, 27, '', '', 1.0, 0, 1, 5, 3, 1, 0, 30, 1)
,(934, 209, 4, '', '', 1.0, 1, 1, 6, 3, 1, 0, 30, 1)
,(935, 209, 32, '', '', 1.0, 1, 0, 7, 3, 1, 0, 30, 1)
,(936, 210, 1, '', '', 1.0, 1, 1, 8, 3, 1, 0, 30, 1)
,(937, 210, 5, '', '', 1.0, 1, 0, 9, 3, 1, 0, 30, 1)
,(938, 210, 28, '', '', 1.0, 0, 1, 10, 3, 1, 0, 30, 1)
,(939, 210, 30, '', '', 1.0, 1, 1, 11, 3, 1, 0, 30, 1)
,(940, 210, 6, '', '', 1.0, 1, 0, 12, 3, 1, 0, 30, 1)
,(941, 210, 27, '', '', 1.0, 0, 1, 13, 3, 1, 0, 30, 1)
,(942, 210, 4, '', '', 1.0, 1, 1, 14, 3, 1, 0, 30, 1)
,(943, 210, 32, '', '', 1.0, 1, 0, 15, 3, 1, 0, 30, 1);

-- Turn10-Schule
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into disziplin (id, name) values (28, 'Balken') on conflict (id) do nothing;
insert into disziplin (id, name) values (6, 'Reck') on conflict (id) do nothing;
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values
(211, 'Turn10-Schule', 0, null, 211, 0, 100, 'e3f740c0-4b4e-4b0f-9cb5-c884363d4217', 2)
,(212, 'BS', 0, 211, 212, 0, 100, '316ab2c8-6fff-4f4d-b2a4-afd360f63fcd', 2)
,(213, 'OS', 0, 211, 213, 0, 100, '9585596a-a571-414d-9a32-000a1e38c307', 2);
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values
(944, 212, 1, '', '', 1.0, 1, 1, 0, 3, 1, 0, 30, 1)
,(945, 212, 5, '', '', 1.0, 1, 0, 1, 3, 1, 0, 30, 1)
,(946, 212, 28, '', '', 1.0, 0, 1, 2, 3, 1, 0, 30, 1)
,(947, 212, 6, '', '', 1.0, 1, 1, 3, 3, 1, 0, 30, 1)
,(948, 212, 4, '', '', 1.0, 1, 1, 4, 3, 1, 0, 30, 1)
,(949, 213, 1, '', '', 1.0, 1, 1, 5, 3, 1, 0, 30, 1)
,(950, 213, 5, '', '', 1.0, 1, 0, 6, 3, 1, 0, 30, 1)
,(951, 213, 28, '', '', 1.0, 0, 1, 7, 3, 1, 0, 30, 1)
,(952, 213, 6, '', '', 1.0, 1, 1, 8, 3, 1, 0, 30, 1)
,(953, 213, 4, '', '', 1.0, 1, 1, 9, 3, 1, 0, 30, 1);

-- GeTu BLTV
insert into disziplin (id, name) values (6, 'Reck') on conflict (id) do nothing;
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into disziplin (id, name) values (3, 'Ring') on conflict (id) do nothing;
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values
(214, 'GeTu BLTV', 0, null, 214, 0, 100, '244a53f9-f182-4b7f-905b-53d894b6f463', 1)
,(215, 'K1', 0, 214, 215, 0, 10, '5d65d5ee-aa3a-4ca9-b262-6f6c644ef645', 1)
,(216, 'K2', 0, 214, 216, 0, 12, '92ffa6b2-5568-4bd1-87ff-433957ca50e9', 1)
,(217, 'K3', 0, 214, 217, 0, 14, 'd336fdb2-9109-4403-8aff-288c9fae9385', 1)
,(218, 'K4', 0, 214, 218, 0, 16, 'bddb5e4a-9159-4260-b587-5d74cf652d3e', 1)
,(219, 'K5', 0, 214, 219, 0, 100, 'b76a5db7-1438-44f6-b38b-3ff982f120c2', 1)
,(220, 'K6', 0, 214, 220, 0, 100, '5b74c500-0104-4c1e-9bbc-f0c3ed258681', 1)
,(221, 'K7', 0, 214, 221, 0, 100, '41077087-1700-43d4-b507-327b0907d861', 1)
,(222, 'KD', 0, 214, 222, 22, 100, '460ef56f-1b60-4a10-8874-73edbaba022d', 1)
,(223, 'KH', 0, 214, 223, 28, 100, '01119fbf-27b7-4fe8-aad1-7df31972af70', 1);
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values
(954, 215, 6, '', '', 1.0, 1, 1, 0, 3, 0, 0, 30, 1)
,(955, 215, 1, '', '', 1.0, 1, 1, 1, 3, 0, 0, 30, 1)
,(956, 215, 3, '', '', 1.0, 1, 1, 2, 3, 0, 0, 30, 1)
,(957, 215, 4, '', '', 1.0, 1, 1, 3, 3, 0, 0, 30, 1)
,(958, 215, 5, '', '', 1.0, 1, 0, 4, 3, 0, 0, 30, 0)
,(959, 216, 6, '', '', 1.0, 1, 1, 5, 3, 0, 0, 30, 1)
,(960, 216, 1, '', '', 1.0, 1, 1, 6, 3, 0, 0, 30, 1)
,(961, 216, 3, '', '', 1.0, 1, 1, 7, 3, 0, 0, 30, 1)
,(962, 216, 4, '', '', 1.0, 1, 1, 8, 3, 0, 0, 30, 1)
,(963, 216, 5, '', '', 1.0, 1, 0, 9, 3, 0, 0, 30, 0)
,(964, 217, 6, '', '', 1.0, 1, 1, 10, 3, 0, 0, 30, 1)
,(965, 217, 1, '', '', 1.0, 1, 1, 11, 3, 0, 0, 30, 1)
,(966, 217, 3, '', '', 1.0, 1, 1, 12, 3, 0, 0, 30, 1)
,(967, 217, 4, '', '', 1.0, 1, 1, 13, 3, 0, 0, 30, 1)
,(968, 217, 5, '', '', 1.0, 1, 0, 14, 3, 0, 0, 30, 0)
,(969, 218, 6, '', '', 1.0, 1, 1, 15, 3, 0, 0, 30, 1)
,(970, 218, 1, '', '', 1.0, 1, 1, 16, 3, 0, 0, 30, 1)
,(971, 218, 3, '', '', 1.0, 1, 1, 17, 3, 0, 0, 30, 1)
,(972, 218, 4, '', '', 1.0, 1, 1, 18, 3, 0, 0, 30, 1)
,(973, 218, 5, '', '', 1.0, 1, 0, 19, 3, 0, 0, 30, 0)
,(974, 219, 6, '', '', 1.0, 1, 1, 20, 3, 0, 0, 30, 1)
,(975, 219, 1, '', '', 1.0, 1, 1, 21, 3, 0, 0, 30, 1)
,(976, 219, 3, '', '', 1.0, 1, 1, 22, 3, 0, 0, 30, 1)
,(977, 219, 4, '', '', 1.0, 1, 1, 23, 3, 0, 0, 30, 1)
,(978, 219, 5, '', '', 1.0, 1, 0, 24, 3, 0, 0, 30, 0)
,(979, 220, 6, '', '', 1.0, 1, 1, 25, 3, 0, 0, 30, 1)
,(980, 220, 1, '', '', 1.0, 1, 1, 26, 3, 0, 0, 30, 1)
,(981, 220, 3, '', '', 1.0, 1, 1, 27, 3, 0, 0, 30, 1)
,(982, 220, 4, '', '', 1.0, 1, 1, 28, 3, 0, 0, 30, 1)
,(983, 220, 5, '', '', 1.0, 1, 0, 29, 3, 0, 0, 30, 0)
,(984, 221, 6, '', '', 1.0, 1, 1, 30, 3, 0, 0, 30, 1)
,(985, 221, 1, '', '', 1.0, 1, 1, 31, 3, 0, 0, 30, 1)
,(986, 221, 3, '', '', 1.0, 1, 1, 32, 3, 0, 0, 30, 1)
,(987, 221, 4, '', '', 1.0, 1, 1, 33, 3, 0, 0, 30, 1)
,(988, 221, 5, '', '', 1.0, 1, 0, 34, 3, 0, 0, 30, 0)
,(989, 222, 6, '', '', 1.0, 1, 1, 35, 3, 0, 0, 30, 1)
,(990, 222, 1, '', '', 1.0, 1, 1, 36, 3, 0, 0, 30, 1)
,(991, 222, 3, '', '', 1.0, 1, 1, 37, 3, 0, 0, 30, 1)
,(992, 222, 4, '', '', 1.0, 1, 1, 38, 3, 0, 0, 30, 1)
,(993, 222, 5, '', '', 1.0, 1, 0, 39, 3, 0, 0, 30, 0)
,(994, 223, 6, '', '', 1.0, 1, 1, 40, 3, 0, 0, 30, 1)
,(995, 223, 1, '', '', 1.0, 1, 1, 41, 3, 0, 0, 30, 1)
,(996, 223, 3, '', '', 1.0, 1, 1, 42, 3, 0, 0, 30, 1)
,(997, 223, 4, '', '', 1.0, 1, 1, 43, 3, 0, 0, 30, 1)
,(998, 223, 5, '', '', 1.0, 1, 0, 44, 3, 0, 0, 30, 0);
