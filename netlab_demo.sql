/*
  NetLab 完整建库 + 答辩演示数据
  MySQL 8.0

  警告：本脚本会删除 netlab_db 中同名业务表并重新创建。
  仅用于空数据库或允许重置的答辩演示数据库。

  默认管理员：2847894378@qq.com
  默认密码：123456
  登录时仍需通过项目的邮箱验证码。

  学生账号 student01@netlab.demo、student02@netlab.demo 是展示账号，
  邮箱不能真实接收验证码。如需学生登录，请执行完成后把用户名改成可用邮箱。
*/

CREATE DATABASE IF NOT EXISTS `netlab_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `netlab_db`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `persistent_logins`;
DROP TABLE IF EXISTS `review`;
DROP TABLE IF EXISTS `device_fault_record`;
DROP TABLE IF EXISTS `borrow_record`;
DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `device`;
DROP TABLE IF EXISTS `device_category`;
DROP TABLE IF EXISTS `laboratory`;
DROP TABLE IF EXISTS `account`;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. 用户表
-- =========================================================

CREATE TABLE `account` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(32) NOT NULL DEFAULT 'USER',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_username` (`username`),
  CONSTRAINT `ck_account_role`
    CHECK (`role` IN ('ADMIN', 'USER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 2. 实验室表
-- =========================================================

CREATE TABLE `laboratory` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `lab_code` VARCHAR(30) NOT NULL,
  `lab_name` VARCHAR(100) NOT NULL,
  `location` VARCHAR(100) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_laboratory_code` (`lab_code`),
  CONSTRAINT `ck_laboratory_status`
    CHECK (`status` IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 3. 设备分类表
-- =========================================================

CREATE TABLE `device_category` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `category_code` VARCHAR(30) NOT NULL,
  `category_name` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 4. 设备表
-- =========================================================

CREATE TABLE `device` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `asset_code` VARCHAR(50) NOT NULL COMMENT '资产编号',
  `device_name` VARCHAR(100) NOT NULL,
  `category_id` INT NOT NULL,
  `laboratory_id` INT NOT NULL,
  `model` VARCHAR(100) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  `description` TEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_asset_code` (`asset_code`),
  KEY `idx_device_category` (`category_id`),
  KEY `idx_device_laboratory` (`laboratory_id`),
  KEY `idx_device_status` (`status`),
  CONSTRAINT `fk_device_category`
    FOREIGN KEY (`category_id`) REFERENCES `device_category` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_device_laboratory`
    FOREIGN KEY (`laboratory_id`) REFERENCES `laboratory` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `ck_device_status`
    CHECK (`status` IN (
      'AVAILABLE', 'BORROWED', 'DAMAGED', 'MAINTENANCE', 'OFFLINE'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 5. 预约表
-- =========================================================

CREATE TABLE `reservation` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `reservation_code` VARCHAR(32) NOT NULL,
  `applicant_id` INT NOT NULL,
  `device_id` INT NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `purpose` VARCHAR(500) NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `reviewer_id` INT NULL,
  `review_time` DATETIME NULL,
  `reject_reason` VARCHAR(500) NULL,
  `cancel_reason` VARCHAR(500) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reservation_code` (`reservation_code`),
  KEY `idx_reservation_applicant_created` (`applicant_id`, `created_at`),
  KEY `idx_reservation_device_status_time`
    (`device_id`, `status`, `start_time`, `end_time`),
  KEY `idx_reservation_reviewer` (`reviewer_id`),
  CONSTRAINT `fk_reservation_applicant`
    FOREIGN KEY (`applicant_id`) REFERENCES `account` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_reservation_device`
    FOREIGN KEY (`device_id`) REFERENCES `device` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_reservation_reviewer`
    FOREIGN KEY (`reviewer_id`) REFERENCES `account` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `ck_reservation_status`
    CHECK (`status` IN (
      'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED',
      'BORROWED', 'RETURN_PENDING', 'COMPLETED'
    )),
  CONSTRAINT `ck_reservation_time`
    CHECK (`start_time` < `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 6. 借用记录表
-- =========================================================

CREATE TABLE `borrow_record` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `borrow_code` VARCHAR(32) NOT NULL,
  `reservation_id` INT NOT NULL,
  `borrow_operator_id` INT NOT NULL,
  `return_operator_id` INT NULL,
  `borrowed_at` DATETIME NOT NULL,
  `due_at` DATETIME NOT NULL,
  `requested_at` DATETIME NULL,
  `return_at` DATETIME NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'BORROWED',
  `remark` VARCHAR(500) NULL,
  `overdue` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_borrow_code` (`borrow_code`),
  UNIQUE KEY `uk_borrow_reservation` (`reservation_id`),
  KEY `idx_borrow_operator` (`borrow_operator_id`),
  KEY `idx_return_operator` (`return_operator_id`),
  KEY `idx_borrow_status_due` (`status`, `due_at`),
  KEY `idx_borrow_overdue` (`overdue`),
  CONSTRAINT `fk_borrow_reservation`
    FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_borrow_operator`
    FOREIGN KEY (`borrow_operator_id`) REFERENCES `account` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_return_operator`
    FOREIGN KEY (`return_operator_id`) REFERENCES `account` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `ck_borrow_status`
    CHECK (`status` IN ('BORROWED', 'RETURN_PENDING', 'RETURNED')),
  CONSTRAINT `ck_borrow_overdue`
    CHECK (`overdue` IN (0, 1)),
  CONSTRAINT `ck_borrow_due_time`
    CHECK (`borrowed_at` <= `due_at`),
  CONSTRAINT `ck_borrow_request_time`
    CHECK (`requested_at` IS NULL OR `requested_at` >= `borrowed_at`),
  CONSTRAINT `ck_borrow_return_time`
    CHECK (`return_at` IS NULL OR `return_at` >= `borrowed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 7. 设备故障记录表
-- =========================================================

CREATE TABLE `device_fault_record` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `device_id` INT NOT NULL,
  `reporter_id` INT NOT NULL,
  `borrow_record_id` INT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `description` VARCHAR(500) NOT NULL,
  `reported_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_fault_device_status` (`device_id`, `status`),
  KEY `idx_fault_reporter` (`reporter_id`),
  KEY `idx_fault_borrow_record` (`borrow_record_id`),
  CONSTRAINT `fk_fault_device`
    FOREIGN KEY (`device_id`) REFERENCES `device` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_fault_reporter`
    FOREIGN KEY (`reporter_id`) REFERENCES `account` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_fault_borrow_record`
    FOREIGN KEY (`borrow_record_id`) REFERENCES `borrow_record` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `ck_fault_status`
    CHECK (`status` IN ('PENDING', 'PROCESSING', 'RESOLVED')),
  CONSTRAINT `ck_fault_resolved_time`
    CHECK (`resolved_at` IS NULL OR `resolved_at` >= `reported_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 8. 首页留言表
-- =========================================================

CREATE TABLE `review` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `account_id` INT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_review_account` (`account_id`),
  CONSTRAINT `fk_review_account`
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 9. Spring Security Remember-Me 表
-- =========================================================

CREATE TABLE `persistent_logins` (
  `username` VARCHAR(64) NOT NULL,
  `series` VARCHAR(64) NOT NULL,
  `token` VARCHAR(64) NOT NULL,
  `last_used` TIMESTAMP NOT NULL,
  PRIMARY KEY (`series`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 10. 操作日志表
-- =========================================================

CREATE TABLE `operation_log` (
 `id` INT NOT NULL AUTO_INCREMENT,
 `business_type` VARCHAR(32) NOT NULL,
 `business_id` INT NOT NULL,
 `operator_id` INT NOT NULL,
 `operation` VARCHAR(64) NOT NULL,
 `previous_status` VARCHAR(32) NULL,
 `next_status` VARCHAR(32) NOT NULL,
 `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (`id`),
 KEY `idx_operation_business`
     (`business_type`, `business_id`, `created_at`),
 KEY `idx_operation_operator` (`operator_id`),
 CONSTRAINT `fk_operation_operator`
     FOREIGN KEY (`operator_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- 11. 基础用户（密码全部为 123456）
-- =========================================================

INSERT INTO `account` (`id`, `username`, `password`, `role`) VALUES
(1, 'admin01@netlab.demo',
 '$2a$10$pRuP23kDLqm68Xi9mG5KxOgeQkLazmFN5XP6eaiB5sdhmB4ZxrjSK', 'ADMIN'),
(2, 'student01@netlab.demo',
 '$2a$10$pRuP23kDLqm68Xi9mG5KxOgeQkLazmFN5XP6eaiB5sdhmB4ZxrjSK', 'USER'),
(3, 'student02@netlab.demo',
 '$2a$10$pRuP23kDLqm68Xi9mG5KxOgeQkLazmFN5XP6eaiB5sdhmB4ZxrjSK', 'USER');

-- =========================================================
-- 12. 实验室与设备分类
-- =========================================================

INSERT INTO `laboratory`
(`id`, `lab_code`, `lab_name`, `location`, `status`) VALUES
(1, 'LAB-01', '网络实验室一', '信息楼 A301', 'ENABLED'),
(2, 'LAB-02', '网络实验室二', '信息楼 A302', 'ENABLED');

INSERT INTO `device_category`
(`id`, `category_code`, `category_name`) VALUES
(1, 'ROUTER', '路由器'),
(2, 'SWITCH', '交换机'),
(3, 'ACCESS_POINT', '无线 AP'),
(4, 'OPTICAL_MODULE', '光模块'),
(5, 'CABLE_TESTER', '网线测试仪'),
(6, 'DEVELOPMENT_BOARD', '开发板'),
(7, 'SERVER', '服务器');

-- =========================================================
-- 13. 演示设备
-- =========================================================

INSERT INTO `device`
(`id`, `asset_code`, `device_name`, `category_id`, `laboratory_id`,
 `model`, `status`, `description`, `created_at`, `updated_at`) VALUES

(1, 'DEMO-RTR-01', 'Cisco 实验路由器', 1, 1,
 'ISR 4321', 'AVAILABLE',
 '用于 OSPF、静态路由和 ACL 配置实验。',
 '2026-10-15 08:00:00', '2026-10-15 08:00:00'),

(2, 'DEMO-SW-01', '华为三层交换机', 2, 1,
 'S5735-L24T4S-A', 'AVAILABLE',
 '并发审批演示设备，存在两条时间重叠的待审核预约。',
 '2026-10-15 08:05:00', '2026-10-15 08:05:00'),

(3, 'DEMO-SW-02', 'Cisco 二层交换机', 2, 1,
 'Catalyst 2960-X', 'AVAILABLE',
 '已审核通过，可在答辩现场演示管理员确认借出。',
 '2026-10-15 08:10:00', '2026-10-15 08:10:00'),

(4, 'DEMO-AP-01', '华为无线接入点', 3, 2,
 'AirEngine 5761-11', 'BORROWED',
 '当前借出中，同时存在一条学生上报的故障记录。',
 '2026-10-15 08:15:00', '2026-10-15 08:15:00'),

(5, 'DEMO-TESTER-01', '智能网线测试仪', 5, 2,
 'NF-8209', 'BORROWED',
 '学生已逾期申请归还，等待管理员确认实物。',
 '2026-10-15 08:20:00', '2026-10-15 08:20:00'),

(6, 'DEMO-BOARD-01', '树莓派网络开发板', 6, 2,
 'Raspberry Pi 4B 8GB', 'AVAILABLE',
 '包含一条按时完成的历史借用记录。',
 '2026-10-15 08:25:00', '2026-10-15 08:25:00'),

(7, 'DEMO-SERVER-01', 'Dell 实验服务器', 7, 1,
 'PowerEdge R740', 'MAINTENANCE',
 '管理员巡检发现电源告警，当前维修中。',
 '2026-10-15 08:30:00', '2026-10-15 08:30:00'),

(8, 'DEMO-OPT-01', '万兆单模光模块', 4, 2,
 'SFP-10G-LR', 'DAMAGED',
 '检测到发光功率异常，等待管理员处理。',
 '2026-10-15 08:35:00', '2026-10-15 08:35:00');

-- =========================================================
-- 14. 演示预约
-- =========================================================

INSERT INTO `reservation`
(`id`, `reservation_code`, `applicant_id`, `device_id`,
 `start_time`, `end_time`, `purpose`, `status`,
 `reviewer_id`, `review_time`, `reject_reason`, `cancel_reason`,
 `created_at`, `updated_at`) VALUES

(1, 'DEMO-RSV-PENDING-01', 2, 1,
 '2026-10-17 09:00:00', '2026-10-17 11:00:00',
 '完成 OSPF 单区域组网及路由表验证实验。',
 'PENDING', NULL, NULL, NULL, NULL,
 '2026-10-15 09:00:00', '2026-10-15 09:00:00'),

-- 同一设备的两条重叠预约，用于演示并发审批
(2, 'DEMO-RSV-CONCURRENT-A', 2, 2,
 '2026-10-17 14:00:00', '2026-10-17 16:00:00',
 '配置 VLAN、Trunk 与单臂路由。',
 'PENDING', NULL, NULL, NULL, NULL,
 '2026-10-15 09:10:00', '2026-10-15 09:10:00'),

(3, 'DEMO-RSV-CONCURRENT-B', 3, 2,
 '2026-10-17 15:00:00', '2026-10-17 17:00:00',
 '验证 STP 根桥选举和链路冗余切换。',
 'PENDING', NULL, NULL, NULL, NULL,
 '2026-10-15 09:20:00', '2026-10-15 09:20:00'),

-- 2026-10-16 全天可演示确认借出
(4, 'DEMO-RSV-APPROVED-01', 3, 3,
 '2026-10-16 00:00:00', '2026-10-16 23:59:00',
 '交换机端口安全与 MAC 地址表实验。',
 'APPROVED', 1, '2026-10-15 10:00:00', NULL, NULL,
 '2026-10-15 09:30:00', '2026-10-15 10:00:00'),

(5, 'DEMO-RSV-BORROWED-01', 2, 4,
 '2026-10-15 18:00:00', '2026-10-17 18:00:00',
 '无线局域网 SSID、安全认证和漫游测试。',
 'BORROWED', 1, '2026-10-15 16:00:00', NULL, NULL,
 '2026-10-15 15:30:00', '2026-10-15 18:10:00'),

(6, 'DEMO-RSV-RETURN-PENDING-01', 3, 5,
 '2026-10-14 09:00:00', '2026-10-14 12:00:00',
 '制作并检测六类双绞线，验证线序和连通性。',
 'RETURN_PENDING', 1, '2026-10-13 16:00:00', NULL, NULL,
 '2026-10-13 15:30:00', '2026-10-14 13:30:00'),

(7, 'DEMO-RSV-COMPLETED-ON-TIME', 2, 6,
 '2026-10-12 09:00:00', '2026-10-12 11:00:00',
 '使用 Socket API 完成局域网通信程序实验。',
 'COMPLETED', 1, '2026-10-11 16:00:00', NULL, NULL,
 '2026-10-11 15:00:00', '2026-10-12 11:10:00'),

(8, 'DEMO-RSV-COMPLETED-OVERDUE', 3, 1,
 '2026-10-10 14:00:00', '2026-10-10 16:00:00',
 '静态路由、默认路由及浮动路由综合实验。',
 'COMPLETED', 1, '2026-10-09 16:00:00', NULL, NULL,
 '2026-10-09 15:00:00', '2026-10-10 17:20:00'),

(9, 'DEMO-RSV-REJECTED-01', 3, 8,
 '2026-10-18 09:00:00', '2026-10-18 11:00:00',
 '光纤链路损耗和收发功率测试。',
 'REJECTED', 1, '2026-10-15 14:00:00',
 '设备检测到发光功率异常，当前不可预约。', NULL,
 '2026-10-15 13:30:00', '2026-10-15 14:00:00'),

(10, 'DEMO-RSV-CANCELLED-01', 2, 3,
 '2026-10-19 14:00:00', '2026-10-19 16:00:00',
 '生成树协议链路冗余实验。',
 'CANCELLED', NULL, NULL, NULL,
 '实验小组时间调整，已重新安排。',
 '2026-10-15 14:10:00', '2026-10-15 14:30:00');

-- =========================================================
-- 15. 借用记录
-- =========================================================

INSERT INTO `borrow_record`
(`id`, `borrow_code`, `reservation_id`, `borrow_operator_id`,
 `return_operator_id`, `borrowed_at`, `due_at`, `requested_at`,
 `return_at`, `status`, `remark`, `overdue`) VALUES

(1, 'DEMO-BRW-ACTIVE-01', 5, 1, NULL,
 '2026-10-15 18:10:00', '2026-10-17 18:00:00',
 NULL, NULL, 'BORROWED',
 '设备外观及配件检查正常，已交付学生。', 0),

(2, 'DEMO-BRW-OVERDUE-01', 6, 1, NULL,
 '2026-10-14 09:10:00', '2026-10-14 12:00:00',
 '2026-10-14 13:30:00', NULL, 'RETURN_PENDING',
 '学生逾期 90 分钟提交归还申请，等待管理员验收。', 1),

(3, 'DEMO-BRW-RETURNED-01', 7, 1, 1,
 '2026-10-12 09:10:00', '2026-10-12 11:00:00',
 '2026-10-12 10:45:00', '2026-10-12 11:10:00',
 'RETURNED', '按时申请归还，设备运行正常。', 0),

(4, 'DEMO-BRW-RETURNED-OVERDUE', 8, 1, 1,
 '2026-10-10 14:10:00', '2026-10-10 16:00:00',
 '2026-10-10 17:00:00', '2026-10-10 17:20:00',
 'RETURNED', '逾期 1 小时申请归还，设备验收正常。', 1);

-- =========================================================
-- 16. 故障记录
-- =========================================================

INSERT INTO `device_fault_record`
(`id`, `device_id`, `reporter_id`, `borrow_record_id`, `status`,
 `description`, `reported_at`, `resolved_at`) VALUES

(1, 4, 2, 1, 'PENDING',
 '设备运行约 30 分钟后供电指示灯间歇闪烁，无线连接偶发中断。',
 '2026-10-15 19:00:00', NULL),

(2, 7, 1, NULL, 'PROCESSING',
 '管理员巡检发现冗余电源模块告警，已送修并等待更换配件。',
 '2026-10-14 10:00:00', NULL),

(3, 6, 2, 3, 'RESOLVED',
 '开发板网口接触不稳定，重新固定接口并测试后恢复正常。',
 '2026-10-12 10:00:00', '2026-10-13 09:00:00'),

(4, 8, 1, NULL, 'PENDING',
 '日常检查发现光模块发光功率低于正常范围，等待进一步检测。',
 '2026-10-15 14:00:00', NULL);

-- =========================================================
-- 17. 首页留言
-- =========================================================

INSERT INTO `review` (`id`, `account_id`, `content`) VALUES
(1, 2, '预约流程清晰，设备状态和审核结果都能及时查看。'),
(2, 3, '时间冲突检测很实用，避免了同一设备被重复预约。'),
(3, 1, '请同学们按预约时间领取设备，使用后及时提交归还申请。');

-- =========================================================
-- 18. 初始化结果检查
-- =========================================================

SELECT 'account' AS `table_name`, COUNT(*) AS `row_count` FROM `account`
UNION ALL
SELECT 'laboratory', COUNT(*) FROM `laboratory`
UNION ALL
SELECT 'device_category', COUNT(*) FROM `device_category`
UNION ALL
SELECT 'device', COUNT(*) FROM `device`
UNION ALL
SELECT 'reservation', COUNT(*) FROM `reservation`
UNION ALL
SELECT 'borrow_record', COUNT(*) FROM `borrow_record`
UNION ALL
SELECT 'device_fault_record', COUNT(*) FROM `device_fault_record`
UNION ALL
SELECT 'review', COUNT(*) FROM `review`;

SELECT
  r.`reservation_code`,
  a.`username` AS `applicant`,
  d.`asset_code`,
  r.`status`,
  r.`start_time`,
  r.`end_time`
FROM `reservation` r
JOIN `account` a ON a.`id` = r.`applicant_id`
JOIN `device` d ON d.`id` = r.`device_id`
ORDER BY r.`id`;

/*
  答辩演示：

  1. DEMO-RSV-CONCURRENT-A 与 DEMO-RSV-CONCURRENT-B
     使用相同设备且时间重叠。批准一条后，另一条应提示冲突。

  2. DEMO-RSV-APPROVED-01
     在 2026-10-16 全天可演示管理员确认借出。

  3. DEMO-RSV-RETURN-PENDING-01
     展示逾期申请归还以及管理员确认归还。

  4. DEMO-RSV-COMPLETED-OVERDUE
     展示已完成但保留的历史逾期记录。
*/
