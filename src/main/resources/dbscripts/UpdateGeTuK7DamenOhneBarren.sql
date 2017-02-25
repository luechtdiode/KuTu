-- Korrektur K7-Barren ohne Frauen
UPDATE wettkampfdisziplin
   SET feminim = 0
 WHERE programm_id = 41
   AND disziplin_id = 5
;