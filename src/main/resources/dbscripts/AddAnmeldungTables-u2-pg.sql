CREATE INDEX xjudgeregistration2_pgm ON judgeregistration_pgm (judgeregistration_id);

ALTER TABLE judgeregistration ADD comment VARCHAR(255) DEFAULT '';
ALTER TABLE judgeregistration_pgm ADD comment VARCHAR(255) DEFAULT '';