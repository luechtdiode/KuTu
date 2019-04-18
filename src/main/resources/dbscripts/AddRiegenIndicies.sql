CREATE INDEX `xwertungwettkampfgeraetriege1` ON `wertung` (`wettkampf_id`, `wettkampfdisziplin_id`, `riege`);
CREATE INDEX `xwertungwettkampfgeraetriege2` ON `wertung` (`wettkampf_id`, `wettkampfdisziplin_id`, `riege2`);
CREATE INDEX `xwertungwettkampfgeraetriegen` ON `wertung` (`wettkampf_id`, `wettkampfdisziplin_id`, `riege`, `riege2`);