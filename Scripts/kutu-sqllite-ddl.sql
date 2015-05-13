-- -----------------------------------------------------
-- Table `verein`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `verein`;

CREATE TABLE IF NOT EXISTS `verein` (
  `id` integer primary key,
  `name` varchar(100) NOT NULL
);


-- -----------------------------------------------------
-- Table `athlet`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `athlet`;

CREATE TABLE IF NOT EXISTS `athlet` (
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
  FOREIGN KEY (`verein`) REFERENCES `verein` (`id`)
);

-- -----------------------------------------------------
-- Table `programm`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `programm`;

CREATE TABLE IF NOT EXISTS `programm` (
  `id` integer primary key,
  `name` varchar(100) NOT NULL,
  `aggregate` INT NOT NULL,
  `parent_id` integer,
  `ord` INT NOT NULL DEFAULT 0,
  `alter_von` int NOT NULL DEFAULT 0,
  `alter_bis` int NOT NULL DEFAULT 100,
  FOREIGN KEY (`parent_id`) REFERENCES `programm` (`id`)
);


-- -----------------------------------------------------
-- Table `wettkampf`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `wettkampf`;

CREATE TABLE IF NOT EXISTS `wettkampf` (
  `id` integer primary key,
  `datum` date NOT NULL,
  `titel` varchar(100) NOT NULL,
  `programm_id` integer NOT NULL,
  `auszeichnung` INT NOT NULL DEFAULT 40,
  FOREIGN KEY (`programm_id`) REFERENCES `programm` (`id`)
);


-- -----------------------------------------------------
-- Table `disziplin`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `disziplin`;

CREATE TABLE IF NOT EXISTS `disziplin` (
  `id` integer primary key,
  `name` varchar(100) NOT NULL,
  `ord` INT NOT NULL DEFAULT 0
);


-- -----------------------------------------------------
-- Table `wettkampfdisziplin`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `wettkampfdisziplin`;

CREATE TABLE IF NOT EXISTS `wettkampfdisziplin` (
  `id` integer primary key,
  `programm_id` integer NOT NULL,
  `disziplin_id` integer NOT NULL,
  `kurzbeschreibung` varchar(100) NOT NULL DEFAULT "",
  `detailbeschreibung` BLOB,
  `notenfaktor` decimal(10,3) NOT NULL DEFAULT 1.0,
  FOREIGN KEY (`disziplin_id`) REFERENCES `disziplin` (`id`),
  FOREIGN KEY (`programm_id`) REFERENCES `programm` (`id`)
);

-- -----------------------------------------------------
-- Table `notenskala`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `notenskala`;

CREATE TABLE IF NOT EXISTS `notenskala` (
  `id` integer primary key,
  `wettkampfdisziplin_id` integer NOT NULL,
  `kurzbeschreibung` varchar(500) NOT NULL DEFAULT "",
  `punktwert` decimal(10,3) NOT NULL,
  FOREIGN KEY (`wettkampfdisziplin_id`) REFERENCES `wettkampfdisziplin` (`id`)
);


-- -----------------------------------------------------
-- Table `wertung`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `wertung`;

CREATE TABLE IF NOT EXISTS `wertung` (
  `id` integer primary key,
  `athlet_id` integer NOT NULL,
  `wettkampfdisziplin_id` integer NOT NULL,
  `wettkampf_id` integer NOT NULL,
  `note_d` decimal(10,3) NOT NULL,
  `note_e` decimal(10,3) NOT NULL,
  `endnote` decimal(10,3) NOT NULL,
  FOREIGN KEY (`athlet_id`) REFERENCES `athlet` (`id`),
  FOREIGN KEY (`wettkampfdisziplin_id`) REFERENCES `wettkampfdisziplin` (`id`),
  FOREIGN KEY (`wettkampf_id`) REFERENCES `wettkampf` (`id`)
);