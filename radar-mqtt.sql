/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : radar-mqtt

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 08/04/2026 15:50:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for iot_device
-- ----------------------------
DROP TABLE IF EXISTS `iot_device`;
CREATE TABLE `iot_device`  (
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `device_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备名称',
  `device_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备唯一编码',
  `device_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态（ONLINE/OFFLINE/FAULT）',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备安装位置',
  `protocol_id` bigint NULL DEFAULT NULL COMMENT '通信协议ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '逻辑删除（0-未删除 1-已删除）',
  `meteorology_id` bigint NULL DEFAULT NULL COMMENT '气象设备编号',
  `relay_protocol_id` bigint NULL DEFAULT NULL COMMENT '继电器通信协议ID',
  PRIMARY KEY (`device_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_device
-- ----------------------------
INSERT INTO `iot_device` VALUES (374750517349453824, '第六套设备', 'WD001', '网关', 'online', '201楼道', 374825127600328704, '2025-10-31 10:44:48', '2026-04-07 10:53:43', '0', 387124856124215296, 377282140729118720);

-- ----------------------------
-- Table structure for iot_protocol
-- ----------------------------
DROP TABLE IF EXISTS `iot_protocol`;
CREATE TABLE `iot_protocol`  (
  `protocol_id` bigint NOT NULL COMMENT '协议ID',
  `protocol_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '协议名称（MQTT，Modbus等）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '协议说明',
  `config_json` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '协议配置内容（JSON格式）',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '逻辑删除（0-未删除 1-已删除）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用状态',
  `send_topic` int NULL DEFAULT NULL COMMENT '发生topic编号',
  PRIMARY KEY (`protocol_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通信协议表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_protocol
-- ----------------------------
INSERT INTO `iot_protocol` VALUES (374825127600328704, 'MQTT', '第二套设备协议', '{\"host\":\"tcp://localhost:1883\",\"topic\":\"6\"}', '2025-10-31 15:41:17', NULL, NULL, 'MQTT', '1', 16);
INSERT INTO `iot_protocol` VALUES (377282140729118720, 'TCP', '第六套设备协议', '{\"host\":\"192.168.1.100\",\"topic\":\"5000\"}', '2025-11-07 10:24:34', NULL, NULL, 'TCP', '1', NULL);

-- ----------------------------
-- Table structure for iot_protocol_type
-- ----------------------------
DROP TABLE IF EXISTS `iot_protocol_type`;
CREATE TABLE `iot_protocol_type`  (
  `type_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '协议类型编号',
  `type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '协议类型名称',
  `type_sort` int NULL DEFAULT NULL COMMENT '排序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用状态',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`type_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通信协议类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_protocol_type
-- ----------------------------
INSERT INTO `iot_protocol_type` VALUES ('430558495587307520', 'MQTT', 1, '0', '2026-04-03 10:45:45', NULL);
INSERT INTO `iot_protocol_type` VALUES ('430558641238708224', 'TCP', 2, '0', '2026-04-03 10:46:20', NULL);

-- ----------------------------
-- Table structure for iot_telemetry
-- ----------------------------
DROP TABLE IF EXISTS `iot_telemetry`;
CREATE TABLE `iot_telemetry`  (
  `telemetry_id` bigint NOT NULL COMMENT '数据记录ID',
  `device_id` bigint NULL DEFAULT NULL COMMENT '来源协议ID',
  `data_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据键名',
  `data_value` double NULL DEFAULT NULL COMMENT '数值型数据',
  `unit` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位',
  `record_time` datetime NULL DEFAULT NULL COMMENT '数据录入时间',
  PRIMARY KEY (`telemetry_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '传感器数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_telemetry
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
