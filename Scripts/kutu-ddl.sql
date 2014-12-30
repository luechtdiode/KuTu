
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

CREATE SCHEMA IF NOT EXISTS `kutu` DEFAULT CHARACTER SET utf8;
USE `kutu`;

-- -----------------------------------------------------
-- Table `kutu`.`verein`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kutu`.`verein`;

CREATE TABLE IF NOT EXISTS `kutu`.`verein` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  UNIQUE INDEX `id` (`id` ASC) ,
  PRIMARY KEY (`id`)
) 
ENGINE=InnoDB 
DEFAULT CHARSET=utf8;


-- -----------------------------------------------------
-- Table `kutu`.`athlet`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kutu`.`athlet`;

CREATE TABLE IF NOT EXISTS `kutu`.`athlet` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(35) NOT NULL,
  `vorname` varchar(35) NOT NULL,
  `gebdat` date DEFAULT NULL,
  `verein` bigint(20) DEFAULT NULL,
  UNIQUE INDEX `id` (`id` ASC) ,
  PRIMARY KEY (`id`),
  CONSTRAINT `athlet_Verein_FK` 
    FOREIGN KEY (`verein`) 
    REFERENCES `verein` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) 
ENGINE=InnoDB
DEFAULT CHARSET=utf8;


-- -----------------------------------------------------
-- Table `kutu`.`programm`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kutu`.`programm`;

CREATE TABLE IF NOT EXISTS `kutu`.`programm` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `parent_id` bigint(20),
  UNIQUE INDEX `id` (`id` ASC),
  PRIMARY KEY (`id`),
  CONSTRAINT `programm_parent_FK` 
    FOREIGN KEY (`parent_id`) 
    REFERENCES `kutu`.`programm` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) 
ENGINE=InnoDB
DEFAULT CHARSET=utf8;


-- -----------------------------------------------------
-- Table `kutu`.`wettkampf`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kutu`.`wettkampf`;

CREATE TABLE IF NOT EXISTS `kutu`.`wettkampf` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `datum` date NOT NULL,
  `titel` varchar(100) NOT NULL,
  `programm_id` bigint(20) NOT NULL,
  UNIQUE INDEX `id` (`id` ASC),
  PRIMARY KEY (`id`),
  CONSTRAINT `wettkampf_programm_FK` 
    FOREIGN KEY (`programm_id`) 
    REFERENCES `kutu`.`programm` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) 
ENGINE=InnoDB 
DEFAULT CHARSET=utf8;


-- -----------------------------------------------------
-- Table `kutu`.`disziplin`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kutu`.`disziplin`;

CREATE TABLE IF NOT EXISTS `kutu`.`disziplin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  UNIQUE INDEX `id` (`id` ASC) ,
  PRIMARY KEY (`id`)
) 
ENGINE=InnoDB
DEFAULT CHARSET=utf8;


-- -----------------------------------------------------
-- Table `kutu`.`wettkampfdisziplin`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kutu`.`wettkampfdisziplin`;

CREATE TABLE IF NOT EXISTS `kutu`.`wettkampfdisziplin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `programm_id` bigint(20) NOT NULL,
  `disziplin_id` bigint(20) NOT NULL,
  `kurzbeschreibung` varchar(100) NOT NULL DEFAULT "",
  `detailbeschreibung` BLOB,
  UNIQUE INDEX `id` (`id` ASC),
  PRIMARY KEY (`id`),
  CONSTRAINT `wettkampfdisziplin_disziplin_FK` 
    FOREIGN KEY (`disziplin_id`) 
    REFERENCES `kutu`.`disziplin` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `wettkampfdisziplin_programm_FK` 
    FOREIGN KEY (`programm_id`) 
    REFERENCES `kutu`.`programm` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8;


-- -----------------------------------------------------
-- Table `kutu`.`wertung`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kutu`.`wertung`;

CREATE TABLE IF NOT EXISTS `kutu`.`wertung` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `athlet_id` bigint(20) NOT NULL,
  `wettkampfdisziplin_id` bigint(20) NOT NULL,
  `wettkampf_id` bigint(20) NOT NULL,
  `note_d` decimal(4,3) NOT NULL,
  `note_e` decimal(4,3) NOT NULL,
  `endnote` decimal(4,3) NOT NULL,
  UNIQUE INDEX `id` (`id` ASC),
  PRIMARY KEY (`id`),
  CONSTRAINT `wertung_athlet_FK` 
    FOREIGN KEY (`athlet_id`) 
    REFERENCES `kutu`.`athlet` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `wertung_wettkampfdisziplin_FK` 
    FOREIGN KEY (`wettkampfdisziplin_id`) 
    REFERENCES `kutu`.`wettkampfdisziplin` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `wertung_wettkampf_FK` 
    FOREIGN KEY (`wettkampf_id`) 
    REFERENCES `kutu`.`wettkampf` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) 
ENGINE=InnoDB 
DEFAULT CHARSET=utf8;


-- ------------------------------------------------------
-- cleanup and reset vars
-- ------------------------------------------------------
USE `kutu`;
SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;