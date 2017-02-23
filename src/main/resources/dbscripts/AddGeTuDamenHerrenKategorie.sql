
INSERT INTO programm
(id, parent_id, name, aggregate, ord)
VALUES
 (42, 20, 'KD', 0, 8)
,(43, 20, 'KH', 0, 9)
;

-- KD & KH-Wettkampfgeräte
-- 1. 6: Reck
-- 2. 1: Boden
-- 3. 26: Schaukelring
-- 4. 4: Sprung
-- 5. 5: Barren
-- ID             => Ord
-- 1, 4, 5, 6, 26 => 2, 4, 5, 1, 3
INSERT INTO wettkampfdisziplin
(programm_id, disziplin_id, ord)
VALUES
 (42, 1, 2)
,(42, 4, 4)
,(42, 6, 1)
,(42, 26, 3)
,(43, 1, 2)
,(43, 4, 4)
,(43, 5, 5)
,(43, 6, 1)
,(43, 26, 3)
