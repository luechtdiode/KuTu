USE `kutu`;

DELETE FROM disziplin;

INSERT INTO disziplin
(id, name, ord)
VALUES(1, 'Boden', 1);
INSERT INTO disziplin
(id, name, ord)
VALUES(2, 'Pferd Pauschen', 2);
INSERT INTO disziplin
(id, name, ord)
VALUES(3, 'Ring', 3);
INSERT INTO disziplin
(id, name, ord)
VALUES(4, 'Sprung', 4);
INSERT INTO disziplin
(id, name, ord)
VALUES(5, 'Barren', 5);
INSERT INTO disziplin
(id, name, ord)
VALUES(6, 'Reck', 6);

INSERT INTO disziplin
(id, name, ord)
VALUES(7, 'Arm-Rumpf-Winkel vorlings', 1);

INSERT INTO disziplin
(id, name, ord)
VALUES(8, 'Arm-Rumpf-Winkel rücklings', 2);

INSERT INTO disziplin
(id, name, ord)
VALUES(9, 'Ein- und Ausschultern', 3);

INSERT INTO disziplin
(id, name, ord)
VALUES(10, 'Brücke', 4);

INSERT INTO disziplin
(id, name, ord)
VALUES(11, 'Rumpfbeugen vorwärts', 5);

INSERT INTO disziplin
(id, name, ord)
VALUES(12, 'Querspagat rechts', 6);

INSERT INTO disziplin
(id, name, ord)
VALUES(13, 'Querspagat links', 7);

INSERT INTO disziplin
(id, name, ord)
VALUES(14, 'Seitspagat', 8);

INSERT INTO disziplin
(id, name, ord)
VALUES(15, 'Sprint', 9);

INSERT INTO disziplin
(id, name, ord)
VALUES(16, 'Standweitsprung', 10);

INSERT INTO disziplin
(id, name, ord)
VALUES(17, 'Hangeln / Klettern / Klimmzug', 11);

INSERT INTO disziplin
(id, name, ord)
VALUES(18, 'Beugestütz / Liegestütz', 12);

INSERT INTO disziplin
(id, name, ord)
VALUES(19, 'Handstand', 13);

INSERT INTO disziplin
(id, name, ord)
VALUES(20, 'Schweizer zum Handstand / Wiener zum Handstand', 14);

INSERT INTO disziplin
(id, name, ord)
VALUES(21, 'Manna / Spitzwinkelstütz / Winkelstütz / Hockwinkelstütz', 15);

INSERT INTO disziplin
(id, name, ord)
VALUES(22, 'Beinheben an der Sprossenwand', 16);

INSERT INTO disziplin
(id, name, ord)
VALUES(23, 'Hangwaage rücklings / Sturzhänge', 17);

INSERT INTO disziplin
(id, name, ord)
VALUES(24, 'Stützwaage gehockt', 18);

INSERT INTO disziplin
(id, name, ord)
VALUES(25, 'Pilz Kreisen beider Beine', 19);



DELETE FROM programm;

INSERT INTO programm
(id, parent_id, name, aggregate)
VALUES(1, null, 'Athletiktest', 1);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(2, 1, 'Athletiktest Kraft', 1, 2);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(3, 1, 'Athletiktest Beweglichkeit', 1, 1);

INSERT INTO programm
(id, parent_id, name, aggregate, alter_von, alter_bis, ord)
VALUES(4, 3, 'Athletiktest-Beweglichkeit 8-10 Jährige', 1, 8, 10, 3);

INSERT INTO programm
(id, parent_id, name, aggregate, alter_von, alter_bis, ord)
VALUES(5, 2, 'Athletiktest-Kraft 8-10 Jährige', 1, 8, 10, 4);

INSERT INTO programm
(id, parent_id, name, aggregate, alter_von, alter_bis, ord)
VALUES(6, 3, 'Athletiktest-Beweglichkeit -7 Jährige', 1, 0, 7, 1);

INSERT INTO programm
(id, parent_id, name, aggregate, alter_von, alter_bis, ord)
VALUES(7, 2, 'Athletiktest-Kraft -7 Jährige', 1, 0, 7, 2);

INSERT INTO programm
(id, parent_id, name, aggregate, alter_von, ord)
VALUES(8, 3, 'Athletiktest-Beweglichkeit 11+ Jährige', 1, 11, 5);

INSERT INTO programm
(id, parent_id, name, aggregate, alter_von, ord)
VALUES(9, 2, 'Athletiktest-Kraft 11+ Jährige', 1, 11, 6);

INSERT INTO programm
(id, parent_id, name, aggregate)
VALUES(11, null, 'KuTu-Wettkampf', 0);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(12, 11, 'EP', 0, 1);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(13, 11, 'P1-U9', 0, 2);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(14, 11, 'P1', 0, 3);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(15, 11, 'P2', 0, 4);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(16, 11, 'P3', 0, 5);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(17, 11, 'P4', 0, 6);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(18, 11, 'P5', 0, 7);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(19, 11, 'P6', 0, 8);

INSERT INTO programm
(id, parent_id, name, aggregate)
VALUES(20, null, 'GeTu-Wettkampf', 0);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(21, 20, 'K1', 0, 1);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(22, 20, 'K2', 0, 2);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(23, 20, 'K3', 0, 3);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(24, 20, 'K4', 0, 4);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(25, 20, 'K5', 0, 5);

INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES(26, 20, 'K6', 0, 6);

DELETE FROM wettkampfdisziplin;
-- Athletiktest Beweglichkeit (4, 6, 8 -> 7-14)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 7);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 8);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 9);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 10);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 11);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 12);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 13);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(4, 14);
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
VALUES(8, 7);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(8, 8);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(8, 9);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(8, 10);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(8, 11);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(8, 12);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(8, 13);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(8, 14);
-- Athletiktest Kraft (5,7,9 -> 15-25)
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 15);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 16);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 17);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 18);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 19);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 20);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 21);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 22);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 23);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 24);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(5, 25);
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
VALUES(9, 15);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 16);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 17);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 18);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 19);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 20);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 21);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 22);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 23);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 24);
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id)
VALUES(9, 25);

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
INSERT INTO verein
(name)
VALUES('TV Seltisberg');
INSERT INTO verein
(name)
VALUES('TV Nunningen');
INSERT INTO verein
(name)
VALUES('AS Pratteln');
INSERT INTO verein
(name)
VALUES('NKL');

DELETE FROM athlet;
INSERT INTO athlet
(name, vorname, gebdat, verein)
VALUES('Weihofen', 'Yannik', '2005-06-29', 1);
