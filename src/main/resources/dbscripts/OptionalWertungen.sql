CREATE TABLE IF NOT EXISTS `wertung_2` (
  `id` integer primary key,
  `athlet_id` integer NOT NULL,
  `wettkampfdisziplin_id` integer NOT NULL,
  `wettkampf_id` integer NOT NULL,
  `note_d` decimal(10,3) DEFAULT NULL,
  `note_e` decimal(10,3) DEFAULT NULL,
  `endnote` decimal(10,3) DEFAULT NULL,
  `riege` varchar(100) DEFAULT NULL,
  `riege2` varchar(100) DEFAULT NULL,
  FOREIGN KEY (`athlet_id`) REFERENCES `athlet` (`id`),
  FOREIGN KEY (`wettkampfdisziplin_id`) REFERENCES `wettkampfdisziplin` (`id`),
  FOREIGN KEY (`wettkampf_id`) REFERENCES `wettkampf` (`id`)
);
INSERT INTO `wertung_2` SELECT * from `wertung`;
DROP TABLE `wertung`;
ALTER TABLE `wertung_2` RENAME TO `wertung`;
