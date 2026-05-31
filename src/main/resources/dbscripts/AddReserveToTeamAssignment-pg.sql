-- -----------------------------------------------------
-- Reserve assignment for team handling
-- -----------------------------------------------------
ALTER TABLE athletregistration ADD COLUMN IF NOT EXISTS reserve integer;
ALTER TABLE wertung            ADD COLUMN IF NOT EXISTS reserve integer;

