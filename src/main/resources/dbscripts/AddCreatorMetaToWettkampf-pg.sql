-- -----------------------------------------------------
-- Add creator metadata to wettkampfmetadata
-- -----------------------------------------------------
ALTER TABLE wettkampfmetadata ADD COLUMN creator_name varchar(200) DEFAULT NULL;
ALTER TABLE wettkampfmetadata ADD COLUMN creator_address varchar(500) DEFAULT NULL;
ALTER TABLE wettkampfmetadata ADD COLUMN creator_phone varchar(50) DEFAULT NULL;
ALTER TABLE wettkampfmetadata ADD COLUMN terms_accepted boolean DEFAULT FALSE;
ALTER TABLE wettkampfmetadata ADD COLUMN terms_accepted_at timestamp DEFAULT NULL;
ALTER TABLE wettkampfmetadata ADD COLUMN terms_version varchar(20) DEFAULT NULL;
