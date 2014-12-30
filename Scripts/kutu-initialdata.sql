USE `kutu`;

DELETE FROM disziplin;

INSERT INTO disziplin
(id, name)
VALUES(4, 'Sprung');
INSERT INTO disziplin
(id, name)
VALUES(2, 'Pferd Pauschen');
INSERT INTO disziplin
(id, name)
VALUES(5, 'Barren');
INSERT INTO disziplin
(id, name)
VALUES(3, 'Ring');
INSERT INTO disziplin
(id, name)
VALUES(1, 'Boden');
INSERT INTO disziplin
(id, name)
VALUES(6, 'Reck');

INSERT INTO disziplin
(id, name)
VALUES(7, 'Arm-Rumpf-Winkel vorlings');

INSERT INTO disziplin
(id, name)
VALUES(8, 'Arm-Rumpf-Winkel rücklings');

INSERT INTO disziplin
(id, name)
VALUES(9, 'Ein- und Ausschultern');

INSERT INTO disziplin
(id, name)
VALUES(10, 'Brücke');

INSERT INTO disziplin
(id, name)
VALUES(11, 'Rumpfbeugen vorwärts');

INSERT INTO disziplin
(id, name)
VALUES(12, 'Querspagat rechts');

INSERT INTO disziplin
(id, name)
VALUES(13, 'Querspagat links');

INSERT INTO disziplin
(id, name)
VALUES(14, 'Seitspagat');

INSERT INTO disziplin
(id, name)
VALUES(15, 'Sprint');

INSERT INTO disziplin
(id, name)
VALUES(16, 'Standweitsprung');

INSERT INTO disziplin
(id, name)
VALUES(17, 'Hangeln / Klettern / Klimmzug');

INSERT INTO disziplin
(id, name)
VALUES(18, 'Beugestütz / Liegestütz');

INSERT INTO disziplin
(id, name)
VALUES(19, 'Handstand');

INSERT INTO disziplin
(id, name)
VALUES(20, 'Schweizer zum Handstand / Wiener zum Handstand');

INSERT INTO disziplin
(id, name)
VALUES(21, 'Manna / Spitzwinkelstütz / Winkelstütz / Hockwinkelstütz');

INSERT INTO disziplin
(id, name)
VALUES(22, 'Beinheben an der Sprossenwand');

INSERT INTO disziplin
(id, name)
VALUES(23, 'Hangwaage rücklings / Sturzhänge');

INSERT INTO disziplin
(id, name)
VALUES(24, 'Stützwaage gehockt');

INSERT INTO disziplin
(id, name)
VALUES(25, 'Pilz Kreisen beider Beine');



DELETE FROM programm;

INSERT INTO programm
(id, parent_id, name)
VALUES(1, null, 'Athletiktest');

INSERT INTO programm
(id, parent_id, name)
VALUES(2, 1, 'Athletiktest 8-10 Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(3, 2, 'Athletiktest-Beweglichkeit 8-10 Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(4, 2, 'Athletiktest-Kraft 8-10 Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(5, 1, 'Athletiktest -7 Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(6, 5, 'Athletiktest-Beweglichkeit -7 Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(7, 5, 'Athletiktest-Kraft -7 Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(8, 1, 'Athletiktest 11+ Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(9, 8, 'Athletiktest-Beweglichkeit 11+ Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(10, 8, 'Athletiktest-Kraft 11+ Jährige');

INSERT INTO programm
(id, parent_id, name)
VALUES(11, null, 'KuTu-Wettkampf');

INSERT INTO programm
(id, parent_id, name)
VALUES(12, 11, 'EP');

INSERT INTO programm
(id, parent_id, name)
VALUES(13, 11, 'P1-U9');

INSERT INTO programm
(id, parent_id, name)
VALUES(14, 11, 'P1');

INSERT INTO programm
(id, parent_id, name)
VALUES(15, 11, 'P2');

INSERT INTO programm
(id, parent_id, name)
VALUES(16, 11, 'P3');

INSERT INTO programm
(id, parent_id, name)
VALUES(17, 11, 'P4');

INSERT INTO programm
(id, parent_id, name)
VALUES(18, 11, 'P5');

INSERT INTO programm
(id, parent_id, name)
VALUES(19, 11, 'P6');

INSERT INTO programm
(id, parent_id, name)
VALUES(20, null, 'GeTu-Wettkampf');

INSERT INTO programm
(id, parent_id, name)
VALUES(21, 20, 'K1');

INSERT INTO programm
(id, parent_id, name)
VALUES(22, 20, 'K2');

INSERT INTO programm
(id, parent_id, name)
VALUES(23, 20, 'K3');

INSERT INTO programm
(id, parent_id, name)
VALUES(24, 20, 'K4');

INSERT INTO programm
(id, parent_id, name)
VALUES(25, 20, 'K5');

INSERT INTO programm
(id, parent_id, name)
VALUES(26, 20, 'K6');

DELETE FROM wettkampfdisziplin;
-- Athletiktest Beweglichkeit (3, 6, 9 -> 7-14)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(3, 7);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(3, 8);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(3, 9);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(3, 10);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(3, 11);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(3, 12);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(3, 13);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(3, 14);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(6, 7);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(6, 8);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(6, 9);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(6, 10);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(6, 11);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(6, 12);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(6, 13);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(6, 14);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 7);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 8);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 9);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 10);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 11);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 12);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 13);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 14);
-- Athletiktest Kraft (4,7,10 -> 15-25)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 15);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 16);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 17);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 18);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 19);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 20);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 21);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 22);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 23);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 24);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 25);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 15);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 16);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 17);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 18);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 19);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 20);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 21);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 22);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 23);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 24);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(7, 25);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 15);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 16);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 17);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 18);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 19);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 20);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 21);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 22);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 23);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 24);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(10, 25);

-- EP-Wettkampfgeräte (12 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(12, 1);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(12, 2);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(12, 3);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(12, 4);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(12, 5);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(12, 6);
-- P1-U9-Wettkampfgeräte (13 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(13, 1);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(13, 2);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(13, 3);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(13, 4);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(13, 5);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(13, 6);
-- P1-Wettkampfgeräte (14 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(14, 1);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(14, 2);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(14, 3);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(14, 4);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(14, 5);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(14, 6);
-- P2-Wettkampfgeräte (15 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(15, 1);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(15, 2);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(15, 3);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(15, 4);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(15, 5);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(15, 6);
-- P3-Wettkampfgeräte (16 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(16, 1);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(16, 2);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(16, 3);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(16, 4);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(16, 5);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(16, 6);
-- P4-Wettkampfgeräte (17 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(17, 1);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(17, 2);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(17, 3);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(17, 4);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(17, 5);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(17, 6);
-- P5-Wettkampfgeräte (18 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(18, 1);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(18, 2);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(18, 3);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(18, 4);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(18, 5);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(18, 6);
-- P6-Wettkampfgeräte (19 -> 1-6)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(19, 1);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(19, 2);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(19, 3);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(19, 4);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(19, 5);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(19, 6);

DELETE FROM verein;
INSERT INTO verein
(name)
VALUES('BTV Basel');

DELETE FROM athlet;
INSERT INTO athlet
(name, vorname, gebdat, verein)
VALUES('Weihofen', 'Yannik', '2006-05-30', 1);
