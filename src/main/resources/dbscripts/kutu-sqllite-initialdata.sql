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
VALUES(31, null, 'KuTuri-Wettkampf', 0);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES
 (32, 31, 'EP', 0, 1)
,(33, 31, 'P1-U9', 0, 2)
,(34, 31, 'P1', 0, 3)
,(35, 31, 'P2', 0, 4)
,(36, 31, 'P3', 0, 5)
,(37, 31, 'P4', 0, 6)
,(38, 31, 'P5', 0, 7)
,(39, 31, 'P6', 0, 8)
,(40, 31, 'OP', 0, 9)
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
,(41, 20, 'K7', 0, 7)
,(42, 20, 'KD', 0, 8)
,(43, 20, 'KH', 0, 9)
;

DELETE FROM wettkampfdisziplin;
-- Athletiktest Beweglichkeit (2 (4, 6, 8) -> 7-14)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, notenfaktor, ord)
VALUES
 (1, 2, 7, 4.000, 1)
,(2, 2, 8, 4.000, 2)
,(3, 2, 9, 4.000, 3)
,(4, 2, 10, 4.000, 4)
,(5, 2, 11, 4.000, 5)
,(6, 2, 12, 4.000, 6)
,(7, 2, 13, 4.000, 7)
,(8, 2, 14, 4.000, 8)
;
-- Athletiktest Kraft (3 (5,7,9) -> 15-25)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, notenfaktor, ord)
VALUES
 (9,  3, 15, 1.000, 10)
,(10, 3, 16, 1.000, 11)
,(11, 3, 17, 1.000, 12)
,(12, 3, 18, 1.000, 13)
,(13, 3, 19, 1.000, 14)
,(14, 3, 20, 1.000, 15)
,(15, 3, 21, 1.000, 16)
,(16, 3, 22, 1.000, 17)
,(17, 3, 23, 1.000, 18)
,(18, 3, 24, 1.000, 19)
,(19, 3, 25, 1.000, 20)
;

-- EP-Wettkampfgeräte (12 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (20, 12, 1, 1)
,(21, 12, 2, 2)
,(22, 12, 3, 3)
,(23, 12, 4, 4)
,(24, 12, 5, 5)
,(25, 12, 6, 6)
;

-- P1-U9-Wettkampfgeräte (13 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (26, 13, 1, 1)
,(27, 13, 2, 2)
,(28, 13, 3, 3)
,(29, 13, 4, 4)
,(30, 13, 5, 5)
,(31, 13, 6, 6)
;

-- P1-Wettkampfgeräte (14 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (32, 14, 1, 1)
,(33, 14, 2, 2)
,(34, 14, 3, 3)
,(35, 14, 4, 4)
,(36, 14, 5, 5)
,(37, 14, 6, 6)
;

-- P2-Wettkampfgeräte (15 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (38, 15, 1, 1)
,(39, 15, 2, 2)
,(40, 15, 3, 3)
,(41, 15, 4, 4)
,(42, 15, 5, 5)
,(43, 15, 6, 6)
;

-- P3-Wettkampfgeräte (16 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (44, 16, 1, 1)
,(45, 16, 2, 2)
,(46, 16, 3, 3)
,(47, 16, 4, 4)
,(48, 16, 5, 5)
,(49, 16, 6, 6)
;

-- P4-Wettkampfgeräte (17 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (50, 17, 1, 1)
,(51, 17, 2, 2)
,(52, 17, 3, 3)
,(53, 17, 4, 4)
,(54, 17, 5, 5)
,(55, 17, 6, 6)
;

-- P5-Wettkampfgeräte (18 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (56, 18, 1, 1)
,(57, 18, 2, 2)
,(58, 18, 3, 3)
,(59, 18, 4, 4)
,(60, 18, 5, 5)
,(61, 18, 6, 6)
;

-- P6-Wettkampfgeräte (19 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (62, 19, 1, 1)
,(63, 19, 2, 2)
,(64, 19, 3, 3)
,(65, 19, 4, 4)
,(66, 19, 5, 5)
,(67, 19, 6, 6)
;

-- OP-Wettkampfgeräte (19 -> 1-6)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (68, 27, 1, 1)
,(69, 27, 2, 2)
,(70, 27, 3, 3)
,(71, 27, 4, 4)
,(72, 27, 5, 5)
,(73, 27, 6, 6)
;

-- K1-K7, KH, KD-Wettkampfgeräte (21 - 26 mit 1-6 + 26)
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord, feminim)
VALUES
 (74, 21, 1, 2, 1)
,(75, 21, 4, 4, 1)
,(76, 21, 5, 5, 0)
,(77, 21, 6, 1, 1)
,(78, 21, 26, 3, 1)
,(79, 22, 1, 2, 1)
,(80, 22, 4, 4, 1)
,(81, 22, 5, 5, 0)
,(82, 22, 6, 1, 1)
,(83, 22, 26, 3, 1)
,(84, 23, 1, 2, 1)
,(85, 23, 4, 4, 1)
,(86, 23, 5, 5, 0)
,(87, 23, 6, 1, 1)
,(88, 23, 26, 3, 1)
,(89, 24, 1, 2, 1)
,(90, 24, 4, 4, 1)
,(91, 24, 5, 5, 0)
,(92, 24, 6, 1, 1)
,(93, 24, 26, 3, 1)
,(94, 25, 1, 2, 1)
,(95, 25, 4, 4, 1)
,(96, 25, 5, 5, 0)
,(97, 25, 6, 1, 1)
,(98, 25, 26, 3, 1)
,(99, 26, 1, 2, 1)
,(100, 26, 4, 4, 1)
,(101, 26, 5, 5, 0)
,(102, 26, 6, 1, 1)
,(103, 26, 26, 3, 1)
,(140, 41, 1, 2, 1)
,(141, 41, 4, 4, 1)
,(142, 41, 5, 5, 0)
,(143, 41, 6, 1, 1)
,(144, 41, 26, 3, 1)
,(145, 42, 1, 2, 1)
,(146, 42, 4, 4, 1)
,(147, 42, 6, 1, 1)
,(148, 42, 26, 3, 1)
,(149, 43, 1, 2, 0)
,(150, 43, 4, 4, 0)
,(151, 43, 5, 5, 0)
,(152, 43, 6, 1, 0)
,(153, 43, 26, 3, 0)
;
-- EP-Wettkampfgeräte (32 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (104, 32, 4, 1)
,(105, 32, 27, 2)
,(106, 32, 28, 3)
,(107, 32, 1, 4)
;

-- P1-U9-Wettkampfgeräte (33 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (108, 33, 4, 1)
,(109, 33, 27, 2)
,(110, 33, 28, 3)
,(111, 33, 1, 4)
;

-- P1-Wettkampfgeräte (34 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (112, 34, 4, 1)
,(113, 34, 27, 2)
,(114, 34, 28, 3)
,(115, 34, 1, 4)
;

-- P2-Wettkampfgeräte (35 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (116, 35, 4, 1)
,(117, 35, 27, 2)
,(118, 35, 28, 3)
,(119, 35, 1, 4)
;

-- P3-Wettkampfgeräte (36 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (120, 36, 4, 1)
,(121, 36, 27, 2)
,(122, 36, 28, 3)
,(123, 36, 1, 4)
;

-- P4-Wettkampfgeräte (37 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (124, 37, 4, 1)
,(125, 37, 27, 2)
,(126, 37, 28, 3)
,(127, 37, 1, 4)
;

-- P5-Wettkampfgeräte (38 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (128, 38, 4, 1)
,(129, 38, 27, 2)
,(130, 38, 28, 3)
,(131, 38, 1, 4)
;

-- P6-Wettkampfgeräte (39 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (132, 39, 4, 1)
,(133, 39, 27, 2)
,(134, 39, 28, 3)
,(135, 39, 1, 4)
;

-- OP-Wettkampfgeräte (40 -> 4,27,28,1)
-- 4 Sprung,
-- 27 Stufenbarren,
-- 28 Balken,
-- 1 Boden,
INSERT INTO wettkampfdisziplin
(id, programm_id, disziplin_id, ord)
VALUES
 (136, 40, 4, 1)
,(137, 40, 27, 2)
,(138, 40, 28, 3)
,(139, 40, 1, 4)
;
