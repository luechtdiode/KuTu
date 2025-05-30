ALTER TABLE programm
    ADD COLUMN bestOfCount integer NOT NULL DEFAULT 0;

UPDATE programm
   SET bestOfCount=4
 WHERE id = 27;
