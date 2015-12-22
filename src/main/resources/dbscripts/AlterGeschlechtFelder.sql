ALTER TABLE wettkampfdisziplin ADD masculin INTEGER DEFAULT 1;
ALTER TABLE wettkampfdisziplin ADD feminim INTEGER DEFAULT 1;
UPDATE wettkampfdisziplin
  set feminim = 0
  WHERE
      programm_id in (21, 22, 23, 24, 25, 26)
  and disziplin_id = 5;