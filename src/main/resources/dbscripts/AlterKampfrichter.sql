-- ALTER TABLE wertung ADD verband varchar(100) DEFAULT NULL;
-- ALTER TABLE wettkampf ADD auszeichnungendnote decimal(10,3) NOT NULL DEFAULT 0;
--  `wettkampf_id` integer NOT NULL,
--  `durchgang` varchar(100),
--  `start` integer,

CREATE TABLE IF NOT EXISTS `kampfrichter` (
  `id` integer primary key,
  `js_id` int DEFAULT NULL,
  `geschlecht` char(1) NOT NULL DEFAULT 'M',
  `name` varchar(35) NOT NULL,
  `vorname` varchar(35) NOT NULL,
  `gebdat` date DEFAULT NULL,
  `strasse` varchar(100) DEFAULT '',
  `plz` varchar(10) DEFAULT '',
  `ort` varchar(100) DEFAULT '',
  `verein` integer DEFAULT NULL,
  `activ` Boolean NOT NULL DEFAULT True,
  FOREIGN KEY (`verein`) REFERENCES `verein` (`id`)
);

CREATE UNIQUE INDEX `xkampfrichterpk` ON `kampfrichter` (`id`);
CREATE INDEX `xkampfrichternameverein` ON `kampfrichter` (`name`, `verein`);

CREATE TABLE IF NOT EXISTS `durchgangstation` (
  `wettkampf_id` integer NOT NULL,
  `durchgang` varchar(100),
  `geraet` integer,
  `d_kampfrichter1` integer,
  `e_kampfrichter1` integer,
  `d_kampfrichter2` integer,
  `e_kampfrichter2` integer,
  CONSTRAINT `durchgangstationpk` PRIMARY KEY (`durchgang`,`geraet`,`wettkampf_id`),
  FOREIGN KEY (`wettkampf_id`) REFERENCES `wettkampf` (`id`),
  FOREIGN KEY (`geraet`) REFERENCES `disziplin` (`id`),
  FOREIGN KEY (`d_kampfrichter1`) REFERENCES `kampfrichter` (`id`),
  FOREIGN KEY (`e_kampfrichter1`) REFERENCES `kampfrichter` (`id`),
  FOREIGN KEY (`d_kampfrichter2`) REFERENCES `kampfrichter` (`id`),
  FOREIGN KEY (`e_kampfrichter2`) REFERENCES `kampfrichter` (`id`)
);

CREATE UNIQUE INDEX `xdurchgangstationpk` ON `durchgangstation` (`wettkampf_id`, `durchgang`, `geraet`);