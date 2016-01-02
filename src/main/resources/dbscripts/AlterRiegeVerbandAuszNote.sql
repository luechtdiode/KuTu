ALTER TABLE verein ADD verband varchar(100) DEFAULT NULL;

ALTER TABLE wettkampf ADD auszeichnungendnote decimal(10,3) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS `riege` (
  `name` varchar(100) NOT NULL,
  `wettkampf_id` integer NOT NULL,
  `durchgang` varchar(100),
  `start` integer,
  CONSTRAINT riege_PK PRIMARY KEY (name,wettkampf_id),
  FOREIGN KEY (`wettkampf_id`) REFERENCES `wettkampf` (`id`)
  FOREIGN KEY (`start`) REFERENCES `disziplin` (`id`)
);
