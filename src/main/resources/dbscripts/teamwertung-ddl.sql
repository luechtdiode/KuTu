-- -----------------------------------------------------
-- Table KandidatRegister
-- -----------------------------------------------------
ALTER TABLE athletregistration ADD team integer NOT NULL DEFAULT 0;

-- -----------------------------------------------------
-- Table wertung
-- -----------------------------------------------------
ALTER TABLE wertung ADD team integer NOT NULL DEFAULT 0;
