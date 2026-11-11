-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: kumao_music
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ad_income`
--

DROP TABLE IF EXISTS `ad_income`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ad_income` (
  `id` bigint NOT NULL,
  `advertiser_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL COMMENT 'EXPOSURE/CLICK/WITHDRAW',
  `amount` int NOT NULL DEFAULT '0',
  `description` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `biz_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_advertiser` (`advertiser_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广告收益';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ad_income`
--

LOCK TABLES `ad_income` WRITE;
/*!40000 ALTER TABLE `ad_income` DISABLE KEYS */;
INSERT INTO `ad_income` VALUES (20001,17001,5,'EXPOSURE',1040,'曝光收益','SUCCESS',NULL,'2026-11-02 19:08:44','2026-11-02 19:08:44'),(20002,17001,5,'CLICK',4100,'点击收益','SUCCESS',NULL,'2026-11-02 19:08:44','2026-11-02 19:08:44');
/*!40000 ALTER TABLE `ad_income` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ad_withdraw`
--

DROP TABLE IF EXISTS `ad_withdraw`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ad_withdraw` (
  `id` bigint NOT NULL,
  `advertiser_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `amount` int NOT NULL DEFAULT '0',
  `account` varchar(200) DEFAULT NULL,
  `pay_type` varchar(20) DEFAULT NULL COMMENT 'ALIPAY/WECHAT',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `remark` varchar(500) DEFAULT NULL,
  `audit_time` varchar(32) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_advertiser` (`advertiser_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广告提现';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ad_withdraw`
--

LOCK TABLES `ad_withdraw` WRITE;
/*!40000 ALTER TABLE `ad_withdraw` DISABLE KEYS */;
/*!40000 ALTER TABLE `ad_withdraw` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-09 19:52:22
