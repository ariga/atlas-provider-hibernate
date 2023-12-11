-- Create "Event" table
CREATE TABLE `Event` (`id` bigint NOT NULL AUTO_INCREMENT, `title` varchar(255) NULL, PRIMARY KEY (`id`)) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Create "Event2" table
CREATE TABLE `Event2` (`id` bigint NOT NULL, `title` varchar(255) NULL, PRIMARY KEY (`id`)) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- Create "Location" table
CREATE TABLE `Location` (`id` bigint NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`)) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
