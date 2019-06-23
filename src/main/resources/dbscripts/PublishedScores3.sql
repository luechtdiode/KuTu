CREATE TABLE IF NOT EXISTS `published_scores2` (
    `id` varchar(70) NOT NULL,
    `title` varchar(100) NOT NULL,
    `query` varchar(500) NOT NULL,
    `published` integer NOT NULL DEFAULT 1,
    `published_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `wettkampf_id` integer NOT NULL,
    PRIMARY KEY (`wettkampf_id`, `id`),
    FOREIGN KEY (`wettkampf_id`) REFERENCES `wettkampf` (`id`)
);

DROP TABLE published_scores;

ALTER TABLE published_scores2
    RENAME TO published_scores;