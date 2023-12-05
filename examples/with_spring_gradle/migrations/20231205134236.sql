-- Create "event" table
CREATE TABLE `event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Create "location" table
CREATE TABLE `location` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Create "other_event" table
CREATE TABLE `other_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
