-- Korrektur der Reihenfolge K1-K6-Wettkampfgeräte (21 - 26 mit 1-6 + 26)
-- 1. 6: Reck
-- 2. 1: Boden
-- 3. 26: Schaukelring
-- 4. 4: Sprung
-- 5. 5: Barren
-- ID             => Ord
-- 1, 4, 5, 6, 26 => 2, 4, 5, 1, 3

UPDATE wettkampfdisziplin
  set ord = 2
  WHERE
      programm_id in (21, 22, 23, 24, 25, 26)
  and disziplin_id = 1;
  
UPDATE wettkampfdisziplin
  set ord = 4
  WHERE
      programm_id in (21, 22, 23, 24, 25, 26)
  and disziplin_id = 4;
  
UPDATE wettkampfdisziplin
  set ord = 5
  WHERE
      programm_id in (21, 22, 23, 24, 25, 26)
  and disziplin_id = 5;
  
UPDATE wettkampfdisziplin
  set ord = 1
  WHERE
      programm_id in (21, 22, 23, 24, 25, 26)
  and disziplin_id = 6;
  
UPDATE wettkampfdisziplin
  set ord = 3
  WHERE
      programm_id in (21, 22, 23, 24, 25, 26)
  and disziplin_id = 26;
