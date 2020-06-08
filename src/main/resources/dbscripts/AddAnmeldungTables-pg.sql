-- -----------------------------------------------------
-- Table VereinRegister
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS vereinregistration (
    id SERIAL PRIMARY KEY,
    wettkampf_id integer NOT NULL REFERENCES wettkampf (id),
    verein_id integer REFERENCES verein (id),
    vereinname varchar(100) NOT NULL,
    verband varchar(100) DEFAULT NULL,
    responsible_name varchar(70) NOT NULL,
    responsible_vorname varchar(70) NOT NULL,
    mobilephone varchar(70) NOT NULL,
    mail varchar(70) NOT NULL,
    secrethash varchar NOT NULL,
    registrationtime TIMESTAMP
);
CREATE UNIQUE INDEX xidentity ON vereinregistration (wettkampf_id,vereinname, verband);

-- -----------------------------------------------------
-- Table KandidatRegister
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS athletregistration (
    id SERIAL PRIMARY KEY,
    vereinregistration_id integer NOT NULL REFERENCES vereinregistration (id),
    athlet_id integer NOT NULL REFERENCES athlet (id),
    geschlecht char(1) NOT NULL DEFAULT 'M',
    name varchar(35) NOT NULL,
    vorname varchar(35) NOT NULL,
    gebdat date DEFAULT NULL,
    program_id integer NOT NULL REFERENCES programm (id),
    registrationtime TIMESTAMP
);
CREATE INDEX xathletregistration ON athletregistration (vereinregistration_id);

-- -----------------------------------------------------
-- Table JudgeRegister
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS judgeregistration (
    id SERIAL PRIMARY KEY,
    vereinregistration_id integer NOT NULL REFERENCES vereinregistration (id),
    geschlecht char(1) NOT NULL DEFAULT 'M',
    name varchar(35) NOT NULL,
    vorname varchar(35) NOT NULL,
    mobilephone varchar(70) NOT NULL,
    mail varchar(70) NOT NULL,
    registrationtime TIMESTAMP
);
CREATE INDEX xjudgeregistration ON judgeregistration (vereinregistration_id);

CREATE TABLE IF NOT EXISTS judgeregistration_pgm (
    id SERIAL PRIMARY KEY,
    vereinregistration_id integer NOT NULL REFERENCES vereinregistration (id),
    judgeregistration_id integer NOT NULL REFERENCES judgeregistration (id),
    wettkampfdisziplin_id integer NOT NULL REFERENCES wettkampfdisziplin (id)
);
CREATE INDEX xjudgeregistration_pgm ON judgeregistration_pgm (vereinregistration_id);