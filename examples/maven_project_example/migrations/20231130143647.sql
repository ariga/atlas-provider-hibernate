-- Create "Location" table
CREATE TABLE `Location` (
  `id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Create "Location_SEQ" table
CREATE TABLE `Location_SEQ` (
  `next_val` bigint NULL
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Create "Department" table
CREATE TABLE `Department` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  PRIMARY KEY (`id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Create "Employee" table
CREATE TABLE `Employee` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NULL,
  `department_id` bigint NULL,
  PRIMARY KEY (`id`),
  INDEX `FK14tijxqry9ml17nk86sqfp561` (`department_id`),
  CONSTRAINT `FK14tijxqry9ml17nk86sqfp561` FOREIGN KEY (`department_id`) REFERENCES `Department` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
