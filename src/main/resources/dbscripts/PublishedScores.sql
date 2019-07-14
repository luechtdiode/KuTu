CREATE TABLE IF NOT EXISTS `published_scores` (
  `id` integer primary key,
  `title` varchar(100) NOT NULL,
  `query` varchar(500) NOT NULL,
  `wettkampf_id` integer NOT NULL,
  FOREIGN KEY (`wettkampf_id`) REFERENCES `wettkampf` (`id`)
);
