-- Create "project" table
CREATE TABLE `project` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) NULL,
  `name` varchar(255) NOT NULL,
  `department_id` bigint NULL,
  PRIMARY KEY (`id`),
  INDEX `FKlwqvcorghndns8byxet3u2x87` (`department_id`),
  CONSTRAINT `FKlwqvcorghndns8byxet3u2x87` FOREIGN KEY (`department_id`) REFERENCES `Department` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
