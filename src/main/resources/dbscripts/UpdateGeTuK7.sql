INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES
 (41, 20, 'K7', 0, 7)
;

-- K7-Wettkampfgeräte (41 mit 1-6 + 26)
-- ID             => Ord
-- 1, 4, 5, 6, 26 => 2, 4, 5, 1, 3
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (41, 1, 2)
,(41, 4, 4)
,(41, 5, 5)
,(41, 6, 1)
,(41, 26, 3)
;