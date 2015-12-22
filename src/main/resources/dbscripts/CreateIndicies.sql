
CREATE UNIQUE INDEX `xvereinpk` ON `verein` (`id`);

CREATE UNIQUE INDEX `xathletpk` ON `athlet` (`id`);
CREATE INDEX `xathletnameverein` ON `athlet` (`name`, `verein`);

CREATE UNIQUE INDEX `xprogrammpk` ON `programm` (`id`);
CREATE INDEX `xprogrammparent` ON `programm` (`parent_id`);

CREATE UNIQUE INDEX `xwettkampfpk` ON `wettkampf` (`id`);
CREATE INDEX `xwettkampfprogramm` ON `wettkampf` (`programm_id`);

CREATE UNIQUE INDEX `xdisziplinpk` ON `disziplin` (`id`);

CREATE UNIQUE INDEX `xwettkampfdisziplinpk` ON `wettkampfdisziplin` (`id`);
CREATE INDEX `xwettkampfdisziplindisziplin` ON `wettkampfdisziplin` (`disziplin_id`);
CREATE INDEX `xwettkampfdisziplinprogramm`  ON `wettkampfdisziplin` (`programm_id`);
CREATE INDEX `xwettkampfdisziplinprogramm2` ON `wettkampfdisziplin` (`disziplin_id`, `programm_id`);

CREATE UNIQUE INDEX `xwertungpk` ON `wertung` (`id`);
CREATE INDEX `xwertungathlet_id` ON `wertung` (`athlet_id`);
CREATE INDEX `xwertungwettkampfdisziplin_id`  ON `wertung` (`wettkampfdisziplin_id`);
CREATE INDEX `xwertungwettkampf_id` ON `wertung` (`wettkampf_id`);