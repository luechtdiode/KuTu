ALTER TABLE wettkampf ADD uuid varchar(70);

CREATE INDEX `xwettkampfpkuuid` ON `wettkampf` (`uuid`);
