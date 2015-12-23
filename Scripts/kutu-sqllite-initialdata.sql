DELETE FROM disziplin;

INSERT INTO disziplin
(id, name)
VALUES
 (1, 'Boden')
,(2, 'Pferd Pauschen')
,(3, 'Ring')
,(4, 'Sprung')
,(5, 'Barren')
,(6, 'Reck')
;

INSERT INTO disziplin
(id, name)
VALUES
 (7, 'Arm-Rumpf-Winkel vorlings')
,(8, 'Arm-Rumpf-Winkel rücklings')
,(9, 'Ein- und Ausschultern')
,(10, 'Brücke')
,(11, 'Rumpfbeugen vorwärts')
,(12, 'Querspagat rechts')
,(13, 'Querspagat links')
,(14, 'Seitspagat')
,(15, 'Sprint')
;

INSERT INTO disziplin
(id, name)
VALUES
 (16, 'Standweitsprung')
,(17, 'Hangeln / Klettern / Klimmzug')
,(18, 'Beugestütz / Liegestütz')
,(19, 'Handstand')
,(20, 'Schweizer zum Handstand / Wiener zum Handstand')
,(21, 'Manna / Spitzwinkelstütz / Winkelstütz / Hockwinkelstütz')
,(22, 'Beinheben an der Sprossenwand')
,(23, 'Hangwaage rücklings / Sturzhänge')
,(24, 'Stützwaage gehockt')
,(25, 'Pilz Kreisen beider Beine')
;

INSERT INTO disziplin
(id, name)
VALUES
 (26, 'Schaukelringe')
,(27, 'Stufenbarren')
,(28, 'Balken')
,(29, 'Trampolin')
;

DELETE FROM programm;

INSERT INTO programm
(id, parent_id, name, aggregate)
VALUES(1, null, 'Athletiktest', 0);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(2, 1, 'Athletiktest Beweglichkeit', 1, 2);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(3, 1, 'Athletiktest Kraft', 1, 1);

INSERT INTO programm
(id, parent_id, name, aggregate)
VALUES(11, null, 'KuTu-Wettkampf', 0);

INSERT INTO programm
(id, parent_id, name, aggregate)
VALUES(31, null, 'KuTuri-Wettkampf', 0);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES
 (12, 11, 'EP', 0, 1)
,(13, 11, 'P1-U9', 0, 2)
,(14, 11, 'P1', 0, 3)
,(15, 11, 'P2', 0, 4)
,(16, 11, 'P3', 0, 5)
,(17, 11, 'P4', 0, 6)
,(18, 11, 'P5', 0, 7)
,(19, 11, 'P6', 0, 8)
,(27, 11, 'OP', 0, 9)
;

INSERT INTO programm
(id, parent_id, name, aggregate)
VALUES(20, null, 'GeTu-Wettkampf', 0);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES
 (21, 20, 'K1', 0, 1)
,(22, 20, 'K2', 0, 2)
,(23, 20, 'K3', 0, 3)
,(24, 20, 'K4', 0, 4)
,(25, 20, 'K5', 0, 5)
,(26, 20, 'K6', 0, 6)
;

DELETE FROM wettkampfdisziplin;
-- Athletiktest Beweglichkeit (2 (4, 6, 8) -> 7-14)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, notenfaktor, ord)
VALUES
 (2, 7, 4.000, 1)
,(2, 8, 4.000, 2)
,(2, 9, 4.000, 3)
,(2, 10, 4.000, 4)
,(2, 11, 4.000, 5)
,(2, 12, 4.000, 6)
,(2, 13, 4.000, 7)
,(2, 14, 4.000, 8)
;
-- Athletiktest Kraft (3 (5,7,9) -> 15-25)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, notenfaktor, ord)
VALUES
 (3, 15, 1.000, 10)
,(3, 16, 1.000, 11)
,(3, 17, 1.000, 12)
,(3, 18, 1.000, 13)
,(3, 19, 1.000, 14)
,(3, 20, 1.000, 15)
,(3, 21, 1.000, 16)
,(3, 22, 1.000, 17)
,(3, 23, 1.000, 18)
,(3, 24, 1.000, 19)
,(3, 25, 1.000, 20)
;

-- EP-Wettkampfgeräte (12 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (12, 1, 1)
,(12, 2, 2)
,(12, 3, 3)
,(12, 4, 4)
,(12, 5, 5)
,(12, 6, 6)
;

-- P1-U9-Wettkampfgeräte (13 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (13, 1, 1)
,(13, 2, 2)
,(13, 3, 3)
,(13, 4, 4)
,(13, 5, 5)
,(13, 6, 6)
;

-- P1-Wettkampfgeräte (14 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (14, 1, 1)
,(14, 2, 2)
,(14, 3, 3)
,(14, 4, 4)
,(14, 5, 5)
,(14, 6, 6)
;

-- P2-Wettkampfgeräte (15 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (15, 1, 1)
,(15, 2, 2)
,(15, 3, 3)
,(15, 4, 4)
,(15, 5, 5)
,(15, 6, 6)
;

-- P3-Wettkampfgeräte (16 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (16, 1, 1)
,(16, 2, 2)
,(16, 3, 3)
,(16, 4, 4)
,(16, 5, 5)
,(16, 6, 6)
;

-- P4-Wettkampfgeräte (17 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (17, 1, 1)
,(17, 2, 2)
,(17, 3, 3)
,(17, 4, 4)
,(17, 5, 5)
,(17, 6, 6)
;

-- P5-Wettkampfgeräte (18 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (18, 1, 1)
,(18, 2, 2)
,(18, 3, 3)
,(18, 4, 4)
,(18, 5, 5)
,(18, 6, 6)
;

-- P6-Wettkampfgeräte (19 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (19, 1, 1)
,(19, 2, 2)
,(19, 3, 3)
,(19, 4, 4)
,(19, 5, 5)
,(19, 6, 6)
;

-- OP-Wettkampfgeräte (19 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (27, 1, 1)
,(27, 2, 2)
,(27, 3, 3)
,(27, 4, 4)
,(27, 5, 5)
,(27, 6, 6)
;

-- K1-K6-Wettkampfgeräte (21 - 26 mit 1-6 + 26)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (21, 1, 2)
,(21, 4, 4)
,(21, 5, 5)
,(21, 6, 1)
,(21, 26, 3)
,(22, 1, 2)
,(22, 4, 4)
,(22, 5, 5)
,(22, 6, 1)
,(22, 26, 3)
,(23, 1, 2)
,(23, 4, 4)
,(23, 5, 5)
,(23, 6, 1)
,(23, 26, 3)
,(24, 1, 2)
,(24, 4, 4)
,(24, 5, 5)
,(24, 6, 1)
,(24, 26, 3)
,(25, 1, 2)
,(25, 4, 4)
,(25, 5, 5)
,(25, 6, 1)
,(25, 26, 3)
,(26, 1, 2)
,(26, 4, 4)
,(26, 5, 5)
,(26, 6, 1)
,(26, 26, 3)
;

-- EP-Wettkampfgeräte (32 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (32, 4, 1)
,(32, 27, 2)
,(32, 28, 3)
,(32, 1, 4)
;

-- P1-U9-Wettkampfgeräte (33 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (33, 4, 1)
,(33, 27, 2)
,(33, 28, 3)
,(33, 1, 4)
;

-- P1-Wettkampfgeräte (34 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (34, 4, 1)
,(34, 27, 2)
,(34, 28, 3)
,(34, 1, 4)
;

-- P2-Wettkampfgeräte (35 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (35, 4, 1)
,(35, 27, 2)
,(35, 28, 3)
,(35, 1, 4)
;

-- P3-Wettkampfgeräte (36 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (36, 4, 1)
,(36, 27, 2)
,(36, 28, 3)
,(36, 1, 4)
;

-- P4-Wettkampfgeräte (37 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (37, 4, 1)
,(37, 27, 2)
,(37, 28, 3)
,(37, 1, 4)
;

-- P5-Wettkampfgeräte (38 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (38, 4, 1)
,(38, 27, 2)
,(38, 28, 3)
,(38, 1, 4)
;

-- P6-Wettkampfgeräte (39 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (39, 4, 1)
,(39, 27, 2)
,(39, 28, 3)
,(39, 1, 4)
;

-- OP-Wettkampfgeräte (40 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (40, 4, 1)
,(40, 27, 2)
,(40, 28, 3)
,(40, 1, 4)
;

DELETE FROM verein;
INSERT INTO verein
(name)
VALUES
 ('BTV Basel')
,('TV Seltisberg')
,('TV Nunningen')
,('AS Pratteln')
,('NKL')
;

DELETE FROM athlet;
-- INSERT INTO athlet
-- (name, vorname, gebdat, verein)
-- VALUES('x', 'y', '2005-06-29', 1);


DELETE FROM notenskala;
