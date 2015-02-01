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
--INSERT INTO athlet
--(name, vorname, gebdat, verein)
--VALUES('x', 'y', '2005-06-29', 1);


DELETE FROM notenskala;

-- ARW vorlings
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
(1,7,'ARW > 180° - Rücken gestreckt - BRW klein',12.000)
,(2,7,'ARW > 180° - Rücken gestreckt - BRW klein',11.000)
,(3,7,'ARW = 180° - Rücken gestreckt - BRW klein',10.000)
,(4,7,'ARW = 180° - Rücken gestreckt - BRW klein',9.000)
,(5,7,'ARW < 180° - Rücken gestreckt - BRW klein',8.000)
,(6,7,'ARW < 180° - Rücken gestreckt - BRW klein',7.000)
,(7,7,'ARW < 180° - Rücken leicht gekrümmt - BRW klein',6.000)
,(8,7,'ARW < 180° - Rücken leicht gekrümmt - BRW klein',5.000)
,(9,7,'ARW < 180° - Rücken gekrümmt - BRW klein',4.000)
,(10,7,'ARW < 180° - Rücken gekrümmt - BRW klein',3.000)
,(11,7,'ARW < 180° - Rücken stark gekrümmt - BRW klein',2.000)
,(12,7,'ARW < 180° - Rücken stark gekrümmt - BRW klein',1.000)
;
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
(13,8,'<= 5.0 cm',12.000)
,(14,8,'5.1-6.9cm',11.000)
,(15,8,'7.0-8.9cm',10.000)
,(16,8,'9.0-10.9cm',9.000)
,(17,8,'11.0-12.9cm',8.000)
,(18,8,'13.0-14.9cm',7.000)
,(19,8,'15.0-16.9cm',6.000)
,(20,8,'17.0-18.9cm',5.000)
,(21,8,'19.0-20.9cm',4.000)
,(22,8,'21.0-22.9cm',3.000)
,(23,8,'23.0-24.9cm',2.000)
,(24,8,'25.0-27.0cm',1.000)
,(25,8,'>=27.0cm',0.000)
;
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
(26,9,'Unter Schulterbreite',12.000)
,(27,9,'Unter Schulterbreite',11.000)
,(28,9,'In Schulterbreite',10.000)
,(29,9,'In Schulterbreite',9.000)
,(30,9,'Armstellung 30° u. weniger',8.000)
,(31,9,'Armstellung 30° u. weniger',7.000)
,(32,9,'Armstellung 45° u. weniger',6.000)
,(33,9,'Armstellung 45° u. weniger',5.000)
,(34,9,'Armstellung 45°',4.000)
,(35,9,'Armstellung 45°',3.000)
,(36,9,'Armstellung über 45°',2.000)
,(37,9,'Armstellung über 45°',1.000)
;
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
(38,10,'Schulter hinter den Händen - Beine u. Arme gestreckt - Beine geschlossen',12.000)
,(39,10,'Schulter hinter den Händen - Beine u. Arme gestreckt - Beine geschlossen',11.000)
,(40,10,'Schulter über den Händen - Beine u. Arme gestreckt - Beine geschlossen',10.000)
,(41,10,'Schulter über den Händen - Beine u. Arme gestreckt - Beine geschlossen',9.000)
,(42,10,'Schulter vor den Händen - Beine u. Arme gestreckt - Beine geschlossen',8.000)
,(43,10,'Schulter vor den Händen - Beine u. Arme gestreckt - Beine geschlossen',7.000)
,(44,10,'Schulter vor den Händen - Knie und/oder Arme leicht gebeugt - Beine leicht offen',6.000)
,(45,10,'Schulter vor den Händen - Knie und/oder Arme leicht gebeugt - Beine leicht offen',5.000)
,(46,10,'Schulter vor den Händen - Knie und/oder Arme gebeugt - Beine offen',4.000)
,(47,10,'Schulter vor den Händen - Knie und/oder Arme gebeugt - Beine offen',3.000)
,(48,10,'Schulter vor den Händen - Knie und/oder Arme stark gebeugt - Beine offen',2.000)
,(49,10,'Schulter vor den Händen - Knie und/oder Arme stark gebeugt - Beine offen',1.000)
;
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
,(50,11,'Souveräne Bodenberührung des ganzen Brustbeins - Rücken gestreckt',12.000)
,(51,11,'Souveräne Bodenberührung des ganzen Brustbeins - Rücken gestreckt',11.000)
,(52,11,'Flüchtige Bodenberührung des Brustbeins - Rücken gestreckt',10.000)
,(53,11,'Flüchtige Bodenberührung des Brustbeins - Rücken gestreckt',9.000)
,(54,11,'Knapp keine Bodenberührung des Brustbeins - Rücken gestreckt',8.000)
,(55,11,'Knapp keine Bodenberührung des Brustbeins - Rücken gestreckt',7.000)
,(56,11,'Keine Bodenberührung des Brustbeins - Rücken leicht gekrümmt',6.000)
,(57,11,'Keine Bodenberührung des Brustbeins - Rücken leicht gekrümmt',5.000)
,(58,11,'Brustbein eindeutig vom Boden entfernt - Rücken gekrümmt',4.000)
,(59,11,'Brustbein eindeutig vom Boden entfernt - Rücken gekrümmt',3.000)
,(60,11,'Brustbein weit vom Boden entfernt - Rücken stark gekrümmt - Kopf nach vorne geneigt',2.000)
,(61,11,'Brustbein weit vom Boden entfernt - Rücken stark gekrümmt - Kopf nach vorne geneigt',1.000)
;
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
,(62,12,'Beine auf einer Linie - Hint. Bein nach unten - Kein oder min. Schrittspalt - Hüfte in 90°-45° zur Beinlinie',12.000)
,(63,12,'Beine auf einer Linie - Hint. Bein nach unten - Kein oder min. Schrittspalt - Hüfte in 90°-45° zur Beinlinie',11.000)
,(64,12,'Beine auf einer Linie - Hint. Bein leicht ausgedreht - Minimaler Schrittspalt - Hüfte in 90°-45° zur Beinlinie',10.000)
,(65,12,'Beine auf einer Linie - Hint. Bein leicht ausgedreht - Minimaler Schrittspalt - Hüfte in 90°-45° zur Beinlinie',9.000)
,(66,12,'Beine beinahe auf einer Linie - Hint. Bein leicht ausgedreht od. leicht gebeugt - Kleiner Schrittspalt - Hüfte in <45° zur Beinlinie',8.000)
,(67,12,'Beine beinahe auf einer Linie - Hint. Bein leicht ausgedreht od. leicht gebeugt - Kleiner Schrittspalt - Hüfte in <45° zur Beinlinie',7.000)
,(68,12,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Minimaler Schrittspalt - Hüfte in <45° zur Beinlinie',6.000)
,(69,12,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Minimaler Schrittspalt - Hüfte in <45° zur Beinlinie',5.000)
,(70,12,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie',4.000)
,(71,12,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie',3.000)
,(72,12,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und stark gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie',2.000)
,(73,12,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und stark gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie',1.000)
;
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
,(74,13,'Beine auf einer Linie - Hint. Bein nach unten - Kein oder min. Schrittspalt - Hüfte in 90°-45° zur Beinlinie',12.000)
,(75,13,'Beine auf einer Linie - Hint. Bein nach unten - Kein oder min. Schrittspalt - Hüfte in 90°-45° zur Beinlinie',11.000)
,(76,13,'Beine auf einer Linie - Hint. Bein leicht ausgedreht - Minimaler Schrittspalt - Hüfte in 90°-45° zur Beinlinie',10.000)
,(77,13,'Beine auf einer Linie - Hint. Bein leicht ausgedreht - Minimaler Schrittspalt - Hüfte in 90°-45° zur Beinlinie',9.000)
,(78,13,'Beine beinahe auf einer Linie - Hint. Bein leicht ausgedreht od. leicht gebeugt - Kleiner Schrittspalt - Hüfte in <45° zur Beinlinie',8.000)
,(79,13,'Beine beinahe auf einer Linie - Hint. Bein leicht ausgedreht od. leicht gebeugt - Kleiner Schrittspalt - Hüfte in <45° zur Beinlinie',7.000)
,(80,13,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Minimaler Schrittspalt - Hüfte in <45° zur Beinlinie',6.000)
,(81,13,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Minimaler Schrittspalt - Hüfte in <45° zur Beinlinie',5.000)
,(82,13,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie',4.000)
,(83,13,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie',3.000)
,(84,13,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und stark gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie',2.000)
,(85,13,'Beine nicht auf einer Linie - Hint. Bein leicht ausgedreht und stark gebeugt - Grosser Schrittspalt - Hüfte in <45° zur Beinlinie',1.000)
;
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
(86,14,'Beine auf einer Linie - Beine sind ausgedreht - Kein Schrittspalt - Rücken gestreckt - Oberkörper aufrecht - Arme in Seithalte',12.000)
,(87,14,'Beine auf einer Linie - Beine sind ausgedreht - Kein Schrittspalt - Rücken gestreckt - Oberkörper aufrecht - Arme in Seithalte',11.000)
,(88,14,'Beine auf einer Linie - Beine sind nicht ausgedreht - Kein Schrittspalt - Rücken gestreckt - Oberkörper aufrecht - Arme in Seithalte',10.000)
,(89,14,'Beine auf einer Linie - Beine sind nicht ausgedreht - Kein Schrittspalt - Rücken gestreckt - Oberkörper aufrecht - Arme in Seithalte',9.000)
,(90,14,'Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Kleiner Schrittspalt - Rücken gestreckt - Oberkörper neigt nach vorne - Hände stützen ev. auf',8.000)
,(91,14,'Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Kleiner Schrittspalt - Rücken gestreckt - Oberkörper neigt nach vorne - Hände stützen ev. auf',7.000)
,(92,14,'Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Kleiner bis mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt nach vorne - Hände stützen ev. auf',6.000)
,(93,14,'Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Kleiner bis mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt nach vorne - Hände stützen ev. auf',5.000)
,(94,14,'Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt stark nach vorne - Hände stützen ev. auf',4.000)
,(95,14,'Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt stark nach vorne - Hände stützen ev. auf',3.000)
,(96,14,'Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt stark nach vorne - Hände stützen auf',2.000)
,(97,14,'Beine nicht auf einer Linie - Beine sind nicht ausgedreht - Mittlerer Schrittspalt - Rücken gestreckt - Oberkörper neigt stark nach vorne - Hände stützen auf',1.000)
;
INSERT INTO notenskala (id,wettkampfdisziplin_id,kurzbeschreibung,punktwert) VALUES 
(290,15,'3.0sek',96.000)
,(291,15,'3.05sek',94.000)
,(292,15,'3.1sek',92.000)
,(293,15,'3.15sek',90.000)
,(294,15,'3.2sek',88.000)
,(295,15,'3.25sek',86.000)
,(296,15,'3.3sek',84.000)
,(297,15,'3.35sek',82.000)
,(298,15,'3.4sek',80.000)
,(299,15,'3.45sek',78.000)
,(300,15,'3.5sek',76.000)
,(301,15,'3.55sek',74.000)
,(302,15,'3.6sek',69.000)
,(303,15,'3.65sek',67.000)
,(304,15,'3.7sek',65.000)
,(305,15,'3.75sek',64.000)
,(306,15,'3.8sek',63.000)
,(307,15,'3.85sek',62.000)
,(308,15,'3.9sek',61.000)
,(309,15,'3.95sek',60.000)
,(310,15,'4.0sek',59.000)
,(311,15,'4.05sek',58.000)
,(312,15,'4.1sek',57.000)
,(313,15,'4.15sek',56.000)
,(314,15,'4.2sek',55.000)
,(315,15,'4.25sek',54.000)
,(316,15,'4.3sek',53.000)
,(317,15,'4.35sek',52.000)
,(318,15,'4.4sek',51.000)
,(319,15,'4.45sek',50.000)
,(320,15,'4.5sek',49.000)
,(321,15,'4.55sek',48.000)
,(322,15,'4.6sek',46.000)
,(323,15,'4.65sek',44.000)
,(324,15,'4.7sek',40.000)
,(325,15,'4.75sek',38.000)
,(326,15,'4.8sek',36.000)
,(327,15,'4.85sek',34.000)
,(328,15,'4.9sek',32.000)
,(329,15,'4.95sek',30.000)
,(330,15,'5.0sek',28.000)
,(331,15,'5.05sek',26.000)
,(332,15,'5.1sek',24.000)
,(333,15,'5.15sek',22.000)
,(334,15,'5.2sek',20.000)
,(335,15,'5.25sek',18.000)
,(336,15,'5.3sek',16.000)
,(337,15,'5.35sek',14.000)
,(338,15,'5.4sek',12.000)
,(339,15,'5.45sek',9.000)
,(340,15,'5.5sek',6.000)
,(341,15,'5.55sek',3.000)
,(342,15,'5.6sek',1.000)
;

