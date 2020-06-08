-- -----------------------------------------------------
-- Table VereinRegister
-- -----------------------------------------------------
CREATE TABLE vereinregistration (
    id INTEGER PRIMARY KEY,
    wettkampf_id integer NOT NULL,
    verein_id integer,
    vereinname varchar(100) NOT NULL,
    verband varchar(100) DEFAULT NULL,
    responsible_name varchar(70) NOT NULL,
    responsible_vorname varchar(70) NOT NULL,
    mobilephone varchar(70) NOT NULL,
    mail varchar(70) NOT NULL,
    registrationtime TIMESTAMP,
    secrethash varchar NOT NULL,
    FOREIGN KEY (wettkampf_id) REFERENCES wettkampf (id),
    FOREIGN KEY (verein_id) REFERENCES verein (id)
);
CREATE UNIQUE INDEX xidentity ON vereinregistration (wettkampf_id,vereinname, verband);

-- -----------------------------------------------------
-- Table KandidatRegister
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS athletregistration (
    id INTEGER PRIMARY KEY,
    vereinregistration_id integer NOT NULL,
    athlet_id integer,
    geschlecht char(1) NOT NULL DEFAULT 'M',
    name varchar(35) NOT NULL,
    vorname varchar(35) NOT NULL,
    gebdat date DEFAULT NULL,
    program_id integer NOT NULL REFERENCES programm (id),
    registrationtime TIMESTAMP,
    FOREIGN KEY (vereinregistration_id) REFERENCES vereinregistration (id),
    FOREIGN KEY (athlet_id) REFERENCES athlet (id)
);
CREATE INDEX xathletregistration ON athletregistration (vereinregistration_id);

-- -----------------------------------------------------
-- Table JudgeRegister
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS judgeregistration (
    id INTEGER PRIMARY KEY,
    vereinregistration_id integer NOT NULL,
    geschlecht char(1) NOT NULL DEFAULT 'M',
    name varchar(35) NOT NULL,
    vorname varchar(35) NOT NULL,
    mobilephone varchar(70) NOT NULL,
    mail varchar(70) NOT NULL,
    registrationtime TIMESTAMP,
    FOREIGN KEY (vereinregistration_id) REFERENCES vereinregistration (id)
);
CREATE INDEX xjudgeregistration ON judgeregistration (vereinregistration_id);

CREATE TABLE IF NOT EXISTS judgeregistration_pgm (
    id INTEGER PRIMARY KEY,
    vereinregistration_id integer NOT NULL,
    judgeregistration_id integer NOT NULL,
    wettkampfdisziplin_id integer NOT NULL,
    FOREIGN KEY (vereinregistration_id) REFERENCES vereinregistration (id),
    FOREIGN KEY (judgeregistration_id) REFERENCES judgeregistration (id),
    FOREIGN KEY (wettkampfdisziplin_id) REFERENCES wettkampfdisziplin (id)
);
CREATE INDEX xjudgeregistration_pgm ON judgeregistration_pgm (vereinregistration_id);