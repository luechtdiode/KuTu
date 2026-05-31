-- -----------------------------------------------------
-- Reserve assignment for team handling
-- -----------------------------------------------------
ALTER TABLE athletregistration ADD COLUMN reserve integer;
ALTER TABLE wertung            ADD COLUMN reserve integer;
