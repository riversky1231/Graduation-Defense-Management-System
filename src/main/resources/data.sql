CREATE DATABASE IF NOT EXISTS defense_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE defense_management;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评语内容',
                            `group_id` bigint NOT NULL COMMENT '所属小组ID',
                            `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`) USING BTREE,
                            INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
                            CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `defense_group` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小组评语表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of comment
-- ----------------------------
INSERT INTO `comment` VALUES (1, '本小组学生整体表现优秀，论文/设计质量高，答辩过程中能够清晰表达研究内容，回答问题准确。', 1, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (2, '本小组学生准备充分，技术实现到位，展示效果良好，建议进一步完善文档。', 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (3, '软件工程实践能力强，代码规范，测试完整，项目具有实际应用价值。', 3, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (4, '学生对软件开发流程理解深入，团队协作意识强，工程文档规范。', 4, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (5, '通信理论基础扎实，实验数据可靠，分析论证充分。', 5, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (6, '硬件设计能力突出，测试验证完整，系统性能达到预期指标。', 6, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (7, '人工智能技术应用创新，算法实现高效，实验结果有说服力。', 7, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (8, '深度学习模型设计合理，训练调优到位，应用场景明确。', 8, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (9, '网络安全意识强，攻防技术扎实，安全方案设计完善。', 9, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (10, '信息安全理论功底深厚，系统实现安全可靠，防护策略有效。', 10, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for defense_group
-- ----------------------------
DROP TABLE IF EXISTS `defense_group`;
CREATE TABLE `defense_group`  (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '小组名称',
                                  `department_id` bigint NOT NULL COMMENT '所属院系ID',
                                  `display_order` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
                                  `score` int NOT NULL DEFAULT 0 COMMENT '小组得分',
                                  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `idx_department_id`(`department_id` ASC) USING BTREE,
                                  CONSTRAINT `defense_group_ibfk_dept` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '答辩小组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of defense_group
-- ----------------------------
INSERT INTO `defense_group` VALUES (1, '计算机第一组', 1, 1, 85, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (2, '计算机第二组', 1, 2, 88, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (3, '软件第一组', 2, 1, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (4, '软件第二组', 2, 2, 87, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (5, '信通第一组', 3, 1, 86, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (6, '信通第二组', 3, 2, 89, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (7, 'AI第一组', 4, 1, 92, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (8, 'AI第二组', 4, 2, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (9, '网安第一组', 5, 1, 88, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (10, '网安第二组', 5, 2, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for defense_group_teacher
-- ----------------------------
DROP TABLE IF EXISTS `defense_group_teacher`;
CREATE TABLE `defense_group_teacher`  (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `group_id` bigint NOT NULL COMMENT '小组ID',
                                          `teacher_id` bigint NOT NULL COMMENT '教师ID',
                                          `is_leader` tinyint NOT NULL DEFAULT 0 COMMENT '是否组长：1-是，0-否',
                                          `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          PRIMARY KEY (`id`) USING BTREE,
                                          UNIQUE INDEX `uk_group_teacher`(`group_id` ASC, `teacher_id` ASC) USING BTREE,
                                          INDEX `idx_group_leader`(`group_id` ASC, `is_leader` ASC) USING BTREE,
                                          INDEX `teacher_id`(`teacher_id` ASC) USING BTREE,
                                          CONSTRAINT `defense_group_teacher_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `defense_group` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                                          CONSTRAINT `defense_group_teacher_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '答辩小组教师关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of defense_group_teacher
-- ----------------------------
INSERT INTO `defense_group_teacher` VALUES (1, 1, 1, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (2, 1, 2, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (3, 2, 2, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (4, 2, 3, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (5, 3, 4, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (6, 3, 5, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (7, 4, 5, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (8, 4, 6, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (9, 5, 7, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (10, 5, 8, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (11, 6, 8, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (12, 6, 9, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (13, 7, 10, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (14, 7, 11, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (15, 8, 11, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (16, 8, 12, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (17, 9, 13, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (18, 9, 14, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (19, 10, 14, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (20, 10, 15, 0, '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for defense_leader
-- ----------------------------
DROP TABLE IF EXISTS `defense_leader`;
CREATE TABLE `defense_leader`  (
                                   `id` bigint NOT NULL AUTO_INCREMENT,
                                   `teacher_id` bigint NOT NULL COMMENT '教师ID',
                                   `year` int NOT NULL COMMENT '年份',
                                   `department_id` bigint NOT NULL COMMENT '院系ID',
                                   `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE INDEX `uk_teacher_year`(`teacher_id` ASC, `year` ASC) USING BTREE,
                                   INDEX `idx_teacher_year`(`teacher_id` ASC, `year` ASC) USING BTREE,
                                   INDEX `idx_department_year`(`department_id` ASC, `year` ASC) USING BTREE,
                                   CONSTRAINT `defense_leader_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                   CONSTRAINT `defense_leader_ibfk_2` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '答辩组长表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of defense_leader
-- ----------------------------
INSERT INTO `defense_leader` VALUES (1, 1, 2024, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (2, 2, 2024, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (3, 4, 2024, 2, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (4, 5, 2024, 2, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (5, 7, 2024, 3, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (6, 8, 2024, 3, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (7, 10, 2024, 4, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (8, 11, 2024, 4, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (9, 13, 2024, 5, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (10, 14, 2024, 5, '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '院系名称',
                               `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '院系代码',
                               `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '院系描述',
                               `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`) USING BTREE,
                               UNIQUE INDEX `code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '院系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of department
-- ----------------------------
INSERT INTO `department` VALUES (1, '计算机科学与技术学院', 'CS', '计算机', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `department` VALUES (2, '软件学院', 'SE', '软件', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `department` VALUES (3, '信息与通信工程学院', 'ICE', '信通', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `department` VALUES (4, '人工智能学院', 'AI', '人工智能', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `department` VALUES (5, '网络空间安全学院', 'NSC', '网安', '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for evaluation_item
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_item`;
CREATE TABLE `evaluation_item`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                    `defense_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PAPER 或 DESIGN',
                                    `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标名称',
                                    `weight` double NULL DEFAULT NULL COMMENT '权值 0-1',
                                    `max_score` int NULL DEFAULT NULL COMMENT '最大分值',
                                    `display_order` int NULL DEFAULT NULL COMMENT '显示顺序',
                                    `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    INDEX `idx_type`(`defense_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评分指标配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of evaluation_item
-- ----------------------------
INSERT INTO `evaluation_item` VALUES (1, 'PAPER', '论文质量', 0.5, 50, 1, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (2, 'PAPER', '自述报告', 0.25, 25, 2, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (3, 'PAPER', '回答问题', 0.25, 25, 3, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (4, 'DESIGN', '设计质量1', 0.15, 15, 1, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (5, 'DESIGN', '设计质量2', 0.15, 15, 2, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (6, 'DESIGN', '设计质量3', 0.15, 15, 3, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (7, 'DESIGN', '自述报告', 0.25, 25, 4, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (8, 'DESIGN', '回答问题1', 0.15, 15, 5, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (9, 'DESIGN', '回答问题2', 0.15, 15, 6, '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for large_group_score
-- ----------------------------
DROP TABLE IF EXISTS `large_group_score`;
CREATE TABLE `large_group_score`  (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `student_id` bigint NOT NULL COMMENT '学生ID（小组第一名）',
                                      `teacher_id` bigint NOT NULL COMMENT '评分教师ID',
                                      `year` int NOT NULL COMMENT '答辩年份',
                                      `score` int NOT NULL COMMENT '大组答辩总分（满分100分）',
                                      `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      UNIQUE INDEX `uk_student_teacher_year`(`student_id` ASC, `teacher_id` ASC, `year` ASC) USING BTREE,
                                      INDEX `idx_student_year`(`student_id` ASC, `year` ASC) USING BTREE,
                                      INDEX `idx_teacher_year`(`teacher_id` ASC, `year` ASC) USING BTREE,
                                      CONSTRAINT `large_group_score_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                                      CONSTRAINT `large_group_score_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '大组答辩成绩表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of large_group_score
-- ----------------------------
INSERT INTO `large_group_score` VALUES (1, 1, 1, 2024, 92, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (2, 1, 2, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (3, 5, 1, 2024, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (4, 5, 3, 2024, 89, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (5, 7, 2, 2024, 93, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (6, 7, 3, 2024, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (7, 11, 4, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (8, 11, 5, 2024, 88, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (9, 19, 4, 2024, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (10, 19, 6, 2024, 89, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (11, 21, 7, 2024, 89, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (12, 21, 8, 2024, 87, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (13, 27, 7, 2024, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (14, 27, 9, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (15, 31, 10, 2024, 92, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (16, 31, 11, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (17, 40, 10, 2024, 93, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (18, 40, 12, 2024, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (19, 41, 13, 2024, 89, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (20, 41, 14, 2024, 88, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (21, 47, 13, 2024, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (22, 47, 15, 2024, 89, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
                         `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色描述',
                         `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         PRIMARY KEY (`id`) USING BTREE,
                         UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, 'SUPER_ADMIN', '超级管理员', '2025-12-18 12:38:58');
INSERT INTO `role` VALUES (2, 'DEPT_ADMIN', '院系管理员', '2025-12-18 12:38:58');
INSERT INTO `role` VALUES (3, 'DEFENSE_LEADER', '答辩组长', '2025-12-18 12:38:58');
INSERT INTO `role` VALUES (4, 'TEACHER', '教师', '2025-12-18 12:38:58');
INSERT INTO `role` VALUES (5, 'STUDENT', '学生', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for student_comment
-- ----------------------------
DROP TABLE IF EXISTS `student_comment`;
CREATE TABLE `student_comment`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                    `student_id` bigint NOT NULL COMMENT '学生ID',
                                    `year` int NOT NULL COMMENT '答辩年份',
                                    `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '答辩小组评语内容',
                                    `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    UNIQUE INDEX `uk_student_year`(`student_id` ASC, `year` ASC) USING BTREE,
                                    INDEX `idx_student`(`student_id` ASC) USING BTREE,
                                    CONSTRAINT `student_comment_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生答辩评语表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_comment
-- ----------------------------
INSERT INTO `student_comment` VALUES (1, 1, 2024, '该生论文研究深入，深度学习模型设计合理，实验数据充分，答辩表现优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (2, 2, 2024, '设计方案完整，系统功能实现到位，用户界面友好，答辩回答问题准确。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (3, 3, 2024, '分布式算法研究扎实，理论分析透彻，实验验证充分，具有较高学术价值。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (4, 4, 2024, '智能家居系统设计创新，IoT技术应用合理，系统运行稳定，演示效果良好。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (5, 5, 2024, '区块链技术研究深入，应用场景明确，论述清晰，具有实际应用价值。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (6, 6, 2024, '移动应用开发规范，跨平台实现完整，用户体验良好，答辩表现出色。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (7, 7, 2024, '云计算研究前沿，强化学习方法创新，实验结果有说服力，论文质量高。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (8, 8, 2024, '电商系统设计完善，后台功能全面，代码质量高，文档规范，推荐优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (9, 9, 2024, '微服务架构理解深入，设计模式应用合理，案例分析充分，答辩表现佳。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (10, 10, 2024, 'NLP技术应用创新，聊天机器人功能完善，用户交互自然，系统响应快速。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (11, 11, 2024, '敏捷开发研究实践性强，项目管理经验丰富，论文结构清晰，答辩优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (12, 12, 2024, 'OA系统设计完整，工作流引擎实现到位，企业应用价值高，推荐良好。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (13, 13, 2024, 'DevOps实践经验丰富，CI/CD流程设计合理，自动化程度高，论文质量佳。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (14, 14, 2024, '直播平台功能完善，多人互动实现流畅，系统性能稳定，用户体验好。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (15, 15, 2024, '测试框架研究深入，自动化方案设计合理，测试覆盖率高，论述清晰。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (16, 16, 2024, '小程序开发规范，社区电商功能完整，用户界面美观，商业价值明显。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (17, 17, 2024, '代码质量研究方法科学，度量指标合理，改进建议可行，答辩表现良好。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (18, 18, 2024, 'HR系统功能全面，Vue.js应用熟练，前后端协作良好，系统运行稳定。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (19, 19, 2024, '低代码平台设计创新，可视化编程实现完整，具有较高的研究和应用价值。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (20, 20, 2024, '医院预约系统设计合理，功能完善，用户体验友好，具有良好的应用前景。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (21, 21, 2024, '5G网络切片研究前沿，资源分配算法创新，仿真结果可靠，论文质量高。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (22, 22, 2024, 'LoRa网关设计合理，低功耗实现到位，测试数据完整，硬件实现规范。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (23, 23, 2024, 'MIMO信道估计研究深入，算法优化有效，理论分析严谨，答辩表现优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (24, 24, 2024, '智能天线系统设计创新，波束成形效果好，测试验证充分，技术实现先进。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (25, 25, 2024, '卫星通信链路分析准确，理论计算可靠，仿真结果与理论吻合，论文规范。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (26, 26, 2024, 'V2X通信模块设计合理，测试场景全面，性能指标达标，具有应用价值。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (27, 27, 2024, '毫米波雷达信号处理研究深入，算法实现高效，自动驾驶应用前景广阔。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (28, 28, 2024, 'SDR平台设计完整，软件定义功能丰富，系统灵活性高，技术实现先进。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (29, 29, 2024, '频谱感知算法研究前沿，检测性能优异，认知无线电应用价值高。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (30, 30, 2024, '能量收集型传感器设计创新，功耗优化到位，续航能力强，应用前景好。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (31, 31, 2024, 'Transformer文本生成研究深入，模型设计创新，生成质量高，论文优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (32, 32, 2024, '智能问答系统功能完善，知识图谱构建合理，问答准确率高，应用价值大。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (33, 33, 2024, 'GNN社交网络研究前沿，关系预测效果好，理论分析充分，答辩表现佳。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (34, 34, 2024, '人脸识别系统准确率高，实时性好，考勤功能完善，系统运行稳定。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (35, 35, 2024, '强化学习游戏AI研究创新，DQN算法改进有效，实验结果有说服力。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (36, 36, 2024, '推荐系统设计合理，协同过滤效果好，用户满意度高，商业应用价值明显。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (37, 37, 2024, 'YOLO目标检测优化有效，检测速度和精度平衡好，实际应用效果佳。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (38, 38, 2024, '语音情感识别系统设计创新，CNN-LSTM模型效果好，识别准确率高。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (39, 39, 2024, '联邦学习隐私保护研究前沿，方案设计安全有效，论文具有较高学术价值。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (40, 40, 2024, '自动驾驶感知系统设计完整，传感器融合效果好，系统可靠性高，优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (41, 41, 2024, '密码协议安全分析深入，形式化验证方法正确，论文严谨规范，答辩优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (42, 42, 2024, '入侵检测系统设计合理，机器学习方法有效，检测准确率高，应用价值大。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (43, 43, 2024, '恶意软件检测研究深入，Android应用分析全面，检测方法创新有效。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (44, 44, 2024, '安全审计平台功能完善，日志分析能力强，系统运行稳定，企业应用价值高。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (45, 45, 2024, '零信任架构研究前沿，实践方案可行，安全性能优异，论文质量高。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (46, 46, 2024, 'WAF设计合理，规则与AI结合创新，防护效果好，系统性能稳定。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (47, 47, 2024, '智能合约安全分析深入，漏洞检测方法有效，区块链安全研究具有前沿性。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (48, 48, 2024, '数据脱敏系统设计完善，脱敏方法合理，隐私保护效果好，应用价值明显。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (49, 49, 2024, 'IoT固件安全研究深入，漏洞分析方法有效，安全建议可行，论文规范。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (50, 50, 2024, '多因素认证系统设计安全可靠，用户体验良好，身份认证方案创新。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for student_final_score
-- ----------------------------
DROP TABLE IF EXISTS `student_final_score`;
CREATE TABLE `student_final_score`  (
                                        `id` bigint NOT NULL AUTO_INCREMENT,
                                        `student_id` bigint NOT NULL COMMENT '学生ID',
                                        `year` int NOT NULL COMMENT '年份',
                                        `advisor_score` int NULL DEFAULT NULL COMMENT '指导教师评定成绩',
                                        `reviewer_score` int NULL DEFAULT NULL COMMENT '评阅人评定成绩',
                                        `final_defense_score` decimal(6, 2) NULL DEFAULT NULL COMMENT '最终答辩成绩',
                                        `total_grade` decimal(6, 2) NULL DEFAULT NULL COMMENT '总评成绩',
                                        `adjustment_factor` decimal(8, 3) NULL DEFAULT NULL COMMENT '调节系数',
                                        `group_avg_score` int NULL DEFAULT NULL COMMENT '小组答辩平均成绩',
                                        `large_group_score` int NULL DEFAULT NULL COMMENT '大组答辩成绩',
                                        `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        UNIQUE INDEX `uk_student_year`(`student_id` ASC, `year` ASC) USING BTREE,
                                        CONSTRAINT `student_final_score_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生最终成绩' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_final_score
-- ----------------------------
INSERT INTO `student_final_score` VALUES (1, 1, 2024, 92, 90, 88.50, 89.80, 1.020, 87, 92, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (2, 2, 2024, 88, 85, 86.00, 86.20, 1.000, 86, 88, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (3, 3, 2024, 90, 89, 87.50, 88.40, 1.010, 86, 90, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (4, 4, 2024, 86, 84, 83.00, 84.00, 0.990, 84, 85, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (5, 5, 2024, 91, 90, 89.00, 89.60, 1.015, 88, 91, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (6, 6, 2024, 89, 87, 85.50, 86.60, 1.005, 85, 88, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (7, 7, 2024, 93, 91, 90.00, 91.00, 1.025, 88, 93, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (8, 8, 2024, 87, 86, 84.50, 85.40, 0.995, 85, 86, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (9, 9, 2024, 88, 87, 86.00, 86.60, 1.000, 86, 87, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (10, 10, 2024, 90, 88, 87.00, 87.80, 1.010, 86, 89, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (11, 11, 2024, 91, 89, 88.00, 88.80, 1.015, 87, 90, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (12, 12, 2024, 87, 85, 84.00, 84.80, 0.995, 84, 86, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (13, 13, 2024, 89, 88, 86.50, 87.30, 1.005, 86, 88, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (14, 14, 2024, 86, 84, 83.50, 84.10, 0.990, 84, 85, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (15, 15, 2024, 90, 89, 87.50, 88.30, 1.010, 87, 89, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (16, 16, 2024, 88, 86, 85.00, 85.80, 1.000, 85, 87, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (17, 17, 2024, 87, 86, 84.50, 85.30, 0.995, 85, 86, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (18, 18, 2024, 89, 87, 86.00, 86.80, 1.005, 86, 88, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (19, 19, 2024, 92, 90, 89.00, 89.80, 1.020, 88, 91, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (20, 20, 2024, 86, 85, 83.50, 84.30, 0.990, 84, 85, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (21, 21, 2024, 90, 89, 87.00, 87.80, 1.010, 86, 89, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (22, 22, 2024, 88, 86, 85.50, 86.10, 1.000, 85, 87, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (23, 23, 2024, 91, 90, 88.50, 89.30, 1.015, 87, 90, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (24, 24, 2024, 87, 85, 84.00, 84.80, 0.995, 84, 86, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (25, 25, 2024, 89, 88, 86.50, 87.30, 1.005, 86, 88, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (26, 26, 2024, 86, 85, 83.50, 84.30, 0.990, 84, 85, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (27, 27, 2024, 92, 90, 89.00, 89.80, 1.020, 88, 91, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (28, 28, 2024, 88, 87, 85.50, 86.30, 1.000, 85, 87, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (29, 29, 2024, 90, 88, 87.00, 87.80, 1.010, 86, 89, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (30, 30, 2024, 87, 86, 84.50, 85.30, 0.995, 85, 86, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (31, 31, 2024, 93, 91, 90.50, 91.10, 1.025, 89, 92, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (32, 32, 2024, 89, 87, 86.00, 86.80, 1.005, 86, 88, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (33, 33, 2024, 91, 90, 88.50, 89.30, 1.015, 87, 90, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (34, 34, 2024, 88, 86, 85.00, 85.80, 1.000, 85, 87, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (35, 35, 2024, 90, 89, 87.50, 88.30, 1.010, 87, 89, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (36, 36, 2024, 87, 85, 84.00, 84.80, 0.995, 84, 86, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (37, 37, 2024, 92, 90, 89.00, 89.80, 1.020, 88, 91, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (38, 38, 2024, 88, 87, 85.50, 86.30, 1.000, 85, 87, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (39, 39, 2024, 91, 89, 88.00, 88.80, 1.015, 87, 90, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (40, 40, 2024, 94, 92, 91.00, 91.80, 1.030, 89, 93, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (41, 41, 2024, 90, 89, 87.50, 88.30, 1.010, 87, 89, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (42, 42, 2024, 88, 86, 85.00, 85.80, 1.000, 85, 87, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (43, 43, 2024, 89, 88, 86.50, 87.30, 1.005, 86, 88, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (44, 44, 2024, 87, 85, 84.00, 84.80, 0.995, 84, 86, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (45, 45, 2024, 91, 90, 88.50, 89.30, 1.015, 87, 90, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (46, 46, 2024, 86, 84, 83.00, 83.80, 0.990, 84, 85, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (47, 47, 2024, 92, 90, 89.00, 89.80, 1.020, 88, 91, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (48, 48, 2024, 88, 87, 85.50, 86.30, 1.000, 85, 87, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (49, 49, 2024, 89, 88, 86.50, 87.30, 1.005, 86, 88, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (50, 50, 2024, 90, 88, 87.00, 87.80, 1.010, 86, 89, '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for student_preference
-- ----------------------------
DROP TABLE IF EXISTS `student_preference`;
CREATE TABLE `student_preference`  (
                                       `id` bigint NOT NULL AUTO_INCREMENT,
                                       `student_id` bigint NOT NULL COMMENT '学生ID',
                                       `year` int NOT NULL COMMENT '答辩年份',
                                       `choice1_teacher_id` bigint NULL DEFAULT NULL COMMENT '第一志愿教师ID',
                                       `choice2_teacher_id` bigint NULL DEFAULT NULL COMMENT '第二志愿教师ID',
                                       `choice3_teacher_id` bigint NULL DEFAULT NULL COMMENT '第三志愿教师ID',
                                       `file1_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '第一志愿PDF路径',
                                       `file2_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '第二志愿PDF路径',
                                       `file3_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '第三志愿PDF路径',
                                       `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-已提交，1-已处理',
                                       `admin_assign_type` tinyint NOT NULL DEFAULT 0 COMMENT '院系管理员分配类型：0-随机 1-指定',
                                       `admin_assigned_teacher_id` bigint NULL DEFAULT NULL COMMENT '院系管理员指定导师ID',
                                       `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (`id`) USING BTREE,
                                       UNIQUE INDEX `uk_student_year`(`student_id` ASC, `year` ASC) USING BTREE,
                                       INDEX `idx_choice1`(`choice1_teacher_id` ASC) USING BTREE,
                                       INDEX `idx_choice2`(`choice2_teacher_id` ASC) USING BTREE,
                                       INDEX `idx_choice3`(`choice3_teacher_id` ASC) USING BTREE,
                                       INDEX `idx_admin_assigned_teacher`(`admin_assigned_teacher_id` ASC) USING BTREE,
                                       CONSTRAINT `student_preference_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                                       CONSTRAINT `student_preference_ibfk_2` FOREIGN KEY (`choice1_teacher_id`) REFERENCES `teacher` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
                                       CONSTRAINT `student_preference_ibfk_3` FOREIGN KEY (`choice2_teacher_id`) REFERENCES `teacher` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
                                       CONSTRAINT `student_preference_ibfk_4` FOREIGN KEY (`choice3_teacher_id`) REFERENCES `teacher` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
                                       CONSTRAINT `student_preference_ibfk_5` FOREIGN KEY (`admin_assigned_teacher_id`) REFERENCES `teacher` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生志愿' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_preference
-- ----------------------------
INSERT INTO `student_preference` VALUES (1, 41, 2024, 13, 14, 15, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (2, 42, 2024, 13, 15, 14, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (3, 43, 2024, 14, 13, 15, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (4, 44, 2024, 15, 13, 14, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (5, 45, 2024, 13, 14, 15, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (6, 46, 2024, 14, 15, 13, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (7, 47, 2024, 15, 14, 13, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (8, 48, 2024, 13, 15, 14, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (9, 49, 2024, 14, 13, 15, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_preference` VALUES (10, 50, 2024, 15, 13, 14, NULL, NULL, NULL, 0, 0, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
                                  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
                                  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置值',
                                  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
                                  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of system_config
-- ----------------------------
INSERT INTO `system_config` VALUES ('CURRENT_DEFENSE_YEAR', '2024', '当前答辩年份', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('DEFENSE_DATE_DAY', '20', '答辩日期-日', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('DEFENSE_DATE_MONTH', '6', '答辩日期-月', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('DEFENSE_DATE_YEAR', '2025', '答辩日期-年', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('DESIGN_PROMPT_TEMPLATE', '请基于设计方案、实现细节与现场表现生成客观评语。', '设计评语提示词', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('GRADE_DATE_DAY', '28', '成绩评定日期-日', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('GRADE_DATE_MONTH', '6', '成绩评定日期-月', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('GRADE_DATE_YEAR', '2025', '成绩评定日期-年', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('PAPER_PROMPT_TEMPLATE', '请根据论文题目、摘要与答辩表现生成简洁有力的评语。', '论文评语提示词', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('QWEN_API_KEY', 'sk-115af915697b44df899340c9e39b13f4', 'QWEN 大模型 API Key', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('TEACHER_MAX_STUDENTS', '5', '教师最多可带学生数量', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('VOLUNTEER_DEADLINE', '2025-06-30 18:00', '志愿互选截止时间', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('LARGE_GROUP_DEADLINE', '2025-07-15 18:00', '大组打分截止时间', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('LARGE_GROUP_ARCHIVED', '0', '大组成绩是否已归档(0未归档/1已归档)', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('GROUP_MAX_STUDENTS', '10', '每答辩小组最大学生人数', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for t_student
-- ----------------------------
DROP TABLE IF EXISTS `t_student`;
CREATE TABLE `t_student`  (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `student_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号',
                              `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姓名',
                              `class_info` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '班级',
                              `department_id` bigint NULL DEFAULT NULL COMMENT '所属院系ID',
                              `advisor_teacher_id` bigint NULL DEFAULT NULL COMMENT '指导教师ID',
                              `reviewer_teacher_id` bigint NULL DEFAULT NULL COMMENT '评阅教师ID',
                              `defense_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '毕业考核类型: PAPER 或 DESIGN',
                              `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '毕业考核题目',
                              `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '毕业考核摘要',
                              `defense_group_id` bigint NULL DEFAULT NULL COMMENT '答辩小组ID',
                              `defense_year` int NULL DEFAULT NULL COMMENT '答辩年份',
                              `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学生联系电话',
                              `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学生邮箱',
                              `defense_date` date NULL DEFAULT NULL COMMENT '答辩日期',
                              `avatar_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像路径',
                              `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`) USING BTREE,
                              UNIQUE INDEX `uk_student_no_year`(`student_no` ASC, `defense_year` ASC) USING BTREE,
                              INDEX `idx_department_year`(`department_id` ASC, `defense_year` ASC) USING BTREE,
                              INDEX `idx_group`(`defense_group_id` ASC) USING BTREE,
                              INDEX `advisor_teacher_id`(`advisor_teacher_id` ASC) USING BTREE,
                              INDEX `reviewer_teacher_id`(`reviewer_teacher_id` ASC) USING BTREE,
                              CONSTRAINT `t_student_ibfk_1` FOREIGN KEY (`advisor_teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                              CONSTRAINT `t_student_ibfk_2` FOREIGN KEY (`reviewer_teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                              CONSTRAINT `t_student_ibfk_3` FOREIGN KEY (`defense_group_id`) REFERENCES `defense_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_student
-- ----------------------------
INSERT INTO `t_student` VALUES (1, '2021CS001', '张伟', '计科2101', 1, 1, 2, 'PAPER', '基于深度学习的图像识别系统', '本文研究了深度学习在图像识别领域的应用。', 1, 2024, '13800001001', 'zhangwei@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (2, '2021CS002', '李娜', '计科2101', 1, 1, 3, 'DESIGN', '在线考试系统设计与实现', '设计并实现了一个基于Web的在线考试平台。', 1, 2024, '13800001002', 'lina@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (3, '2021CS003', '王强', '计科2102', 1, 2, 1, 'PAPER', '分布式系统一致性算法研究', '研究了Raft和Paxos算法的性能对比。', 1, 2024, '13800001003', 'wangqiang@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (4, '2021CS004', '刘芳', '计科2102', 1, 2, 3, 'DESIGN', '智能家居控制系统', '基于物联网的智能家居解决方案。', 1, 2024, '13800001004', 'liufang@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (5, '2021CS005', '陈明', '计科2103', 1, 3, 1, 'PAPER', '区块链技术在供应链中的应用', '探讨区块链在供应链溯源中的实践。', 1, 2024, '13800001005', 'chenming@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (6, '2021CS006', '杨丽', '计科2103', 1, 3, 2, 'DESIGN', '移动端健康管理APP', '基于Flutter的跨平台健康应用开发。', 2, 2024, '13800001006', 'yangli@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (7, '2021CS007', '赵刚', '计科2104', 1, 1, 3, 'PAPER', '云计算资源调度优化研究', '基于强化学习的云资源调度策略。', 2, 2024, '13800001007', 'zhaogang@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (8, '2021CS008', '周洁', '计科2104', 1, 2, 1, 'DESIGN', '电商平台后台管理系统', '基于Spring Boot的电商后台设计。', 2, 2024, '13800001008', 'zhoujie@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (9, '2021CS009', '吴磊', '计科2105', 1, 3, 2, 'PAPER', '微服务架构设计模式', '探讨微服务在大型系统中的应用。', 2, 2024, '13800001009', 'wulei@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (10, '2021CS010', '郑梅', '计科2105', 1, 1, 3, 'DESIGN', '智能客服聊天机器人', 'NLP技术在客服领域的应用。', 2, 2024, '13800001010', 'zhengmei@stu.edu.cn', '2024-06-15', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (11, '2021SE001', '孙浩', '软工2101', 2, 4, 5, 'PAPER', '敏捷开发方法在项目管理中的应用', '研究Scrum在软件项目中的实践效果。', 3, 2024, '13800002001', 'sunhao@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (12, '2021SE002', '钱红', '软工2101', 2, 4, 6, 'DESIGN', '企业级OA系统开发', '基于工作流引擎的办公自动化系统。', 3, 2024, '13800002002', 'qianhong@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (13, '2021SE003', '周亮', '软工2102', 2, 5, 4, 'PAPER', '持续集成与持续交付实践', 'CI/CD在DevOps中的最佳实践。', 3, 2024, '13800002003', 'zhouliang@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (14, '2021SE004', '吴婷', '软工2102', 2, 5, 6, 'DESIGN', '在线教育直播平台', '支持多人互动的直播教学系统。', 3, 2024, '13800002004', 'wuting@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (15, '2021SE005', '郑伟', '软工2103', 2, 6, 4, 'PAPER', '软件测试自动化框架研究', '基于Selenium的自动化测试方案。', 3, 2024, '13800002005', 'zhengwei@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (16, '2021SE006', '王雪', '软工2103', 2, 6, 5, 'DESIGN', '社区团购小程序', '微信小程序的社区电商平台。', 4, 2024, '13800002006', 'wangxue@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (17, '2021SE007', '李军', '软工2104', 2, 4, 6, 'PAPER', '代码质量度量与改进', '静态代码分析工具的应用研究。', 4, 2024, '13800002007', 'lijun@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (18, '2021SE008', '张艳', '软工2104', 2, 5, 4, 'DESIGN', '人力资源管理系统', '基于Vue.js的HR信息化平台。', 4, 2024, '13800002008', 'zhangyan@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (19, '2021SE009', '刘涛', '软工2105', 2, 6, 5, 'PAPER', '低代码平台设计与实现', '可视化编程在企业应用中的探索。', 4, 2024, '13800002009', 'liutao@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (20, '2021SE010', '陈静', '软工2105', 2, 4, 6, 'DESIGN', '医院预约挂号系统', '智慧医疗的线上预约解决方案。', 4, 2024, '13800002010', 'chenjing@stu.edu.cn', '2024-06-16', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (21, '2021ICE001', '黄勇', '信通2101', 3, 7, 8, 'PAPER', '5G网络切片技术研究', '5G网络中的资源分配与优化。', 5, 2024, '13800003001', 'huangyong@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (22, '2021ICE002', '徐丹', '信通2101', 3, 7, 9, 'DESIGN', 'LoRa物联网网关设计', '低功耗广域网络设备的设计与实现。', 5, 2024, '13800003002', 'xudan@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (23, '2021ICE003', '马超', '信通2102', 3, 8, 7, 'PAPER', 'MIMO系统信道估计算法', '大规模MIMO的信道估计优化。', 5, 2024, '13800003003', 'machao@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (24, '2021ICE004', '朱敏', '信通2102', 3, 8, 9, 'DESIGN', '智能天线系统设计', '相控阵天线的波束成形技术。', 5, 2024, '13800003004', 'zhumin@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (25, '2021ICE005', '胡斌', '信通2103', 3, 9, 7, 'PAPER', '卫星通信链路分析', '低轨卫星通信系统的链路预算。', 5, 2024, '13800003005', 'hubin@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (26, '2021ICE006', '郭静', '信通2103', 3, 9, 8, 'DESIGN', '车联网通信模块', 'V2X通信模块的设计与测试。', 6, 2024, '13800003006', 'guojing@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (27, '2021ICE007', '林峰', '信通2104', 3, 7, 9, 'PAPER', '毫米波雷达信号处理', '自动驾驶中的毫米波雷达应用。', 6, 2024, '13800003007', 'linfeng@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (28, '2021ICE008', '何琳', '信通2104', 3, 8, 7, 'DESIGN', '软件定义无线电平台', 'SDR平台的设计与实现。', 6, 2024, '13800003008', 'helin@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (29, '2021ICE009', '罗杰', '信通2105', 3, 9, 8, 'PAPER', '频谱感知算法研究', '认知无线电中的频谱检测技术。', 6, 2024, '13800003009', 'luojie@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (30, '2021ICE010', '谢芳', '信通2105', 3, 7, 9, 'DESIGN', '无线传感网络节点', '能量收集型传感器节点设计。', 6, 2024, '13800003010', 'xiefang@stu.edu.cn', '2024-06-17', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (31, '2021AI001', '邓雷', 'AI2101', 4, 10, 11, 'PAPER', '基于Transformer的文本生成', 'GPT模型在文本生成中的应用。', 7, 2024, '13800004001', 'denglei@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (32, '2021AI002', '韩雪', 'AI2101', 4, 10, 12, 'DESIGN', '智能问答系统', '基于知识图谱的问答机器人。', 7, 2024, '13800004002', 'hanxue@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (33, '2021AI003', '曹明', 'AI2102', 4, 11, 10, 'PAPER', '图神经网络在社交网络中的应用', 'GNN用于社交关系预测。', 7, 2024, '13800004003', 'caoming@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (34, '2021AI004', '蒋丽', 'AI2102', 4, 11, 12, 'DESIGN', '人脸识别考勤系统', '基于深度学习的人脸识别应用。', 7, 2024, '13800004004', 'jiangli@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (35, '2021AI005', '袁博', 'AI2103', 4, 12, 10, 'PAPER', '强化学习在游戏AI中的应用', 'DQN算法在Atari游戏中的实践。', 7, 2024, '13800004005', 'yuanbo@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (36, '2021AI006', '唐倩', 'AI2103', 4, 12, 11, 'DESIGN', '智能推荐系统', '协同过滤与深度学习结合的推荐算法。', 8, 2024, '13800004006', 'tangqian@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (37, '2021AI007', '魏刚', 'AI2104', 4, 10, 12, 'PAPER', '目标检测算法优化', 'YOLO系列算法的改进研究。', 8, 2024, '13800004007', 'weigang@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (38, '2021AI008', '冯婷', 'AI2104', 4, 11, 10, 'DESIGN', '语音情感识别系统', '基于CNN-LSTM的语音情感分析。', 8, 2024, '13800004008', 'fengting@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (39, '2021AI009', '程浩', 'AI2105', 4, 12, 11, 'PAPER', '联邦学习隐私保护', '分布式机器学习中的隐私问题。', 8, 2024, '13800004009', 'chenghao@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (40, '2021AI010', '沈琳', 'AI2105', 4, 10, 12, 'DESIGN', '自动驾驶感知系统', '多传感器融合的自动驾驶方案。', 8, 2024, '13800004010', 'shenlin@stu.edu.cn', '2024-06-18', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (41, '2021NSC001', '许强', '网安2101', 5, 13, 14, 'PAPER', '密码学协议安全分析', '现代密码协议的形式化验证。', 9, 2024, '13800005001', 'xuqiang@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (42, '2021NSC002', '曾蕾', '网安2101', 5, 13, 15, 'DESIGN', '网络入侵检测系统', '基于机器学习的入侵检测方案。', 9, 2024, '13800005002', 'zenglei@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (43, '2021NSC003', '彭涛', '网安2102', 5, 14, 13, 'PAPER', '恶意软件检测技术', 'Android恶意应用检测研究。', 9, 2024, '13800005003', 'pengtao@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (44, '2021NSC004', '崔静', '网安2102', 5, 14, 15, 'DESIGN', '安全审计平台', '企业网络安全审计系统设计。', 9, 2024, '13800005004', 'cuijing@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (45, '2021NSC005', '潘伟', '网安2103', 5, 15, 13, 'PAPER', '零信任架构研究', '零信任安全模型的实践应用。', 9, 2024, '13800005005', 'panwei@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (46, '2021NSC006', '范芳', '网安2103', 5, 15, 14, 'DESIGN', 'Web应用防火墙', '基于规则与AI的WAF设计。', 10, 2024, '13800005006', 'fanfang@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (47, '2021NSC007', '姜明', '网安2104', 5, 13, 15, 'PAPER', '区块链安全机制', '智能合约的安全漏洞分析。', 10, 2024, '13800005007', 'jiangming@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (48, '2021NSC008', '秦丽', '网安2104', 5, 14, 13, 'DESIGN', '数据脱敏系统', '敏感数据保护与脱敏方案。', 10, 2024, '13800005008', 'qinli@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (49, '2021NSC009', '夏刚', '网安2105', 5, 15, 14, 'PAPER', '物联网设备安全', 'IoT设备固件安全分析。', 10, 2024, '13800005009', 'xiagang@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (50, '2021NSC010', '田雪', '网安2105', 5, 13, 15, 'DESIGN', '身份认证系统', '多因素身份认证平台设计。', 10, 2024, '13800005010', 'tianxue@stu.edu.cn', '2024-06-19', NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for teacher
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher`  (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `teacher_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教师编号',
                            `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教师姓名',
                            `department_id` bigint NOT NULL COMMENT '所属院系ID',
                            `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职称',
                            `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
                            `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
                            `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '登录密码(加密)',
                            `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
                            `user_id` bigint NULL DEFAULT NULL COMMENT '关联的用户ID',
                            `avatar_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像路径',
                            `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`) USING BTREE,
                            UNIQUE INDEX `teacher_no`(`teacher_no` ASC) USING BTREE,
                            INDEX `idx_teacher_no`(`teacher_no` ASC) USING BTREE,
                            INDEX `idx_department_id`(`department_id` ASC) USING BTREE,
                            INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                            CONSTRAINT `teacher_ibfk_1` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                            CONSTRAINT `teacher_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher
-- ----------------------------
INSERT INTO `teacher` VALUES (1, 'T001', '张三教授', 1, '教授', 'zhangsan@example.com', '13900010001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 10, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (2, 'T002', '李四副教授', 1, '副教授', 'lisi@example.com', '13900010002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 11, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (3, 'T003', '王五讲师', 1, '讲师', 'wangwu@example.com', '13900010003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 12, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (4, 'T004', '赵六教授', 2, '教授', 'zhaoliu@example.com', '13900020001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 13, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (5, 'T005', '钱七副教授', 2, '副教授', 'qianqi@example.com', '13900020002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 14, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (6, 'T006', '孙八讲师', 2, '讲师', 'sunba@example.com', '13900020003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 15, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (7, 'T007', '周九教授', 3, '教授', 'zhoujiu@example.com', '13900030001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 16, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (8, 'T008', '吴十副教授', 3, '副教授', 'wushi@example.com', '13900030002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 17, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (9, 'T009', '郑十一讲师', 3, '讲师', 'zheng11@example.com', '13900030003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 18, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (10, 'T010', '陈十二教授', 4, '教授', 'chen12@example.com', '13900040001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 19, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (11, 'T011', '林十三副教授', 4, '副教授', 'lin13@example.com', '13900040002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 20, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (12, 'T012', '黄十四讲师', 4, '讲师', 'huang14@example.com', '13900040003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 21, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (13, 'T013', '刘十五教授', 5, '教授', 'liu15@example.com', '13900050001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 22, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (14, 'T014', '杨十六副教授', 5, '副教授', 'yang16@example.com', '13900050002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 23, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (15, 'T015', '徐十七讲师', 5, '讲师', 'xu17@example.com', '13900050003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', 1, 24, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for teacher_profile
-- ----------------------------
DROP TABLE IF EXISTS `teacher_profile`;
CREATE TABLE `teacher_profile`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                    `teacher_id` bigint NOT NULL COMMENT '教师ID',
                                    `research_direction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '研究方向',
                                    `enrollment_requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '招生要求',
                                    `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    UNIQUE INDEX `uk_teacher_id`(`teacher_id` ASC) USING BTREE,
                                    CONSTRAINT `teacher_profile_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师研究方向与招生要求' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher_profile
-- ----------------------------
INSERT INTO `teacher_profile` VALUES (1, 1, '高性能计算与系统优化', '具备良好的算法与系统基础，能够完成研究型课题。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (2, 2, '软件工程与质量保障', '有实际项目经验优先，重视工程规范与文档能力。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (3, 3, '分布式系统与云原生', '熟悉容器与微服务体系，有持续学习能力。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (4, 4, '软件体系结构与模式', '具备面向对象设计与重构经验。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (5, 5, '需求工程与人机交互', '重视用户研究与原型设计能力。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (6, 6, '网络通信与协议分析', '对网络协议栈有兴趣，能进行实验与分析。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (7, 7, '嵌入式系统与物联网', '有硬件/驱动或物联网项目经验优先。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (8, 8, '智能算法与机器学习', '具备基本数学与编程能力，能阅读论文。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (9, 9, '计算机视觉与多媒体', '有图像处理或深度学习实践经历优先。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (10, 10, '人工智能系统与应用', '能独立完成项目，具备良好编码习惯。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (11, 11, '数据挖掘与知识图谱', '对数据分析、NLP有兴趣与实践经验。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (12, 12, '强化学习与智能控制', '具备数学建模与实验能力。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (13, 13, '密码学与网络安全', '对安全攻防和密码协议有兴趣，愿意阅读英文资料。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (14, 14, '系统安全与漏洞分析', '具备基础逆向或安全分析能力。', '2026-02-22 14:11:47');
INSERT INTO `teacher_profile` VALUES (15, 15, '隐私保护与安全审计', '有安全测试或审计相关实践优先。', '2026-02-22 14:11:47');

-- ----------------------------
-- Table structure for teacher_score_record
-- ----------------------------
DROP TABLE IF EXISTS `teacher_score_record`;
CREATE TABLE `teacher_score_record`  (
                                         `id` bigint NOT NULL AUTO_INCREMENT,
                                         `student_id` bigint NOT NULL COMMENT '学生ID',
                                         `defense_group_id` bigint NULL DEFAULT NULL COMMENT '答辩小组ID',
                                         `teacher_id` bigint NOT NULL COMMENT '打分教师ID',
                                         `year` int NOT NULL COMMENT '答辩年份',
                                         `item1_score` int NULL DEFAULT NULL COMMENT '分项1',
                                         `item2_score` int NULL DEFAULT NULL COMMENT '分项2',
                                         `item3_score` int NULL DEFAULT NULL COMMENT '分项3',
                                         `item4_score` int NULL DEFAULT NULL COMMENT '分项4',
                                         `item5_score` int NULL DEFAULT NULL COMMENT '分项5',
                                         `item6_score` int NULL DEFAULT NULL COMMENT '分项6',
                                         `total_score` int NULL DEFAULT NULL COMMENT '总分',
                                         `submit_time` datetime NULL DEFAULT NULL COMMENT '提交时间',
                                         `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`) USING BTREE,
                                         INDEX `idx_student_teacher_year`(`student_id` ASC, `teacher_id` ASC, `year` ASC) USING BTREE,
                                         INDEX `idx_student`(`student_id` ASC) USING BTREE,
                                         INDEX `teacher_id`(`teacher_id` ASC) USING BTREE,
                                         CONSTRAINT `teacher_score_record_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                                         CONSTRAINT `teacher_score_record_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师打分记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher_score_record
-- ----------------------------
INSERT INTO `teacher_score_record` VALUES (1, 1, 1, 1, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-15 10:30:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (2, 1, 1, 2, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-15 10:35:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (3, 2, 1, 1, 2024, 14, 13, 14, 22, 12, 11, 86, '2024-06-15 10:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (4, 2, 1, 2, 2024, 13, 14, 13, 21, 13, 12, 86, '2024-06-15 10:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (5, 3, 1, 1, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2024-06-15 11:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (6, 3, 1, 2, 2024, 43, 21, 21, NULL, NULL, NULL, 85, '2024-06-15 11:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (7, 4, 1, 1, 2024, 13, 13, 14, 21, 12, 11, 84, '2024-06-15 11:15:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (8, 4, 1, 2, 2024, 14, 13, 13, 20, 11, 12, 83, '2024-06-15 11:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (9, 5, 1, 1, 2024, 46, 22, 21, NULL, NULL, NULL, 89, '2024-06-15 11:30:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (10, 5, 1, 2, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-15 11:35:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (11, 6, 2, 2, 2024, 14, 13, 13, 21, 12, 12, 85, '2024-06-15 14:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (12, 6, 2, 3, 2024, 13, 14, 14, 22, 11, 12, 86, '2024-06-15 14:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (13, 7, 2, 2, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-15 14:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (14, 7, 2, 3, 2024, 44, 21, 21, NULL, NULL, NULL, 86, '2024-06-15 14:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (15, 8, 2, 2, 2024, 13, 14, 13, 21, 12, 12, 85, '2024-06-15 14:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (16, 8, 2, 3, 2024, 14, 13, 14, 20, 11, 13, 85, '2024-06-15 14:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (17, 9, 2, 2, 2024, 44, 21, 21, NULL, NULL, NULL, 86, '2024-06-15 15:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (18, 9, 2, 3, 2024, 43, 22, 21, NULL, NULL, NULL, 86, '2024-06-15 15:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (19, 10, 2, 2, 2024, 14, 14, 13, 21, 12, 12, 86, '2024-06-15 15:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (20, 10, 2, 3, 2024, 13, 13, 14, 22, 13, 11, 86, '2024-06-15 15:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (21, 11, 3, 4, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-16 10:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (22, 11, 3, 5, 2024, 43, 21, 22, NULL, NULL, NULL, 86, '2024-06-16 10:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (23, 12, 3, 4, 2024, 13, 14, 13, 21, 12, 11, 84, '2024-06-16 10:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (24, 12, 3, 5, 2024, 14, 13, 14, 20, 11, 12, 84, '2024-06-16 10:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (25, 13, 3, 4, 2024, 44, 21, 21, NULL, NULL, NULL, 86, '2024-06-16 10:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (26, 13, 3, 5, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2024-06-16 10:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (27, 14, 3, 4, 2024, 13, 13, 14, 21, 12, 11, 84, '2024-06-16 11:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (28, 14, 3, 5, 2024, 14, 13, 13, 20, 11, 12, 83, '2024-06-16 11:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (29, 15, 3, 4, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-16 11:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (30, 15, 3, 5, 2024, 43, 21, 22, NULL, NULL, NULL, 86, '2024-06-16 11:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (31, 16, 4, 5, 2024, 14, 13, 14, 21, 12, 11, 85, '2024-06-16 14:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (32, 16, 4, 6, 2024, 13, 14, 13, 20, 12, 13, 85, '2024-06-16 14:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (33, 17, 4, 5, 2024, 43, 21, 21, NULL, NULL, NULL, 85, '2024-06-16 14:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (34, 17, 4, 6, 2024, 44, 21, 20, NULL, NULL, NULL, 85, '2024-06-16 14:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (35, 18, 4, 5, 2024, 14, 13, 14, 21, 12, 12, 86, '2024-06-16 14:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (36, 18, 4, 6, 2024, 13, 14, 13, 22, 11, 13, 86, '2024-06-16 14:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (37, 19, 4, 5, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-16 15:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (38, 19, 4, 6, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-16 15:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (39, 20, 4, 5, 2024, 13, 13, 13, 21, 12, 12, 84, '2024-06-16 15:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (40, 20, 4, 6, 2024, 14, 14, 13, 20, 11, 11, 83, '2024-06-16 15:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (41, 21, 5, 7, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-17 10:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (42, 21, 5, 8, 2024, 43, 22, 21, NULL, NULL, NULL, 86, '2024-06-17 10:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (43, 22, 5, 7, 2024, 14, 13, 13, 21, 12, 12, 85, '2024-06-17 10:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (44, 22, 5, 8, 2024, 13, 14, 14, 22, 11, 12, 86, '2024-06-17 10:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (45, 23, 5, 7, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-17 10:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (46, 23, 5, 8, 2024, 44, 21, 21, NULL, NULL, NULL, 86, '2024-06-17 10:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (47, 24, 5, 7, 2024, 13, 14, 13, 20, 12, 12, 84, '2024-06-17 11:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (48, 24, 5, 8, 2024, 14, 13, 14, 21, 11, 11, 84, '2024-06-17 11:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (49, 25, 5, 7, 2024, 44, 21, 21, NULL, NULL, NULL, 86, '2024-06-17 11:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (50, 25, 5, 8, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-17 11:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (51, 26, 6, 8, 2024, 13, 13, 14, 21, 12, 11, 84, '2024-06-17 14:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (52, 26, 6, 9, 2024, 14, 14, 13, 20, 11, 12, 84, '2024-06-17 14:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (53, 27, 6, 8, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-17 14:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (54, 27, 6, 9, 2024, 46, 21, 22, NULL, NULL, NULL, 89, '2024-06-17 14:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (55, 28, 6, 8, 2024, 14, 13, 13, 21, 12, 12, 85, '2024-06-17 14:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (56, 28, 6, 9, 2024, 13, 14, 14, 22, 11, 12, 86, '2024-06-17 14:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (57, 29, 6, 8, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2024-06-17 15:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (58, 29, 6, 9, 2024, 43, 21, 21, NULL, NULL, NULL, 85, '2024-06-17 15:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (59, 30, 6, 8, 2024, 13, 14, 13, 21, 12, 12, 85, '2024-06-17 15:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (60, 30, 6, 9, 2024, 14, 13, 14, 20, 11, 13, 85, '2024-06-17 15:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (61, 31, 7, 10, 2024, 46, 23, 21, NULL, NULL, NULL, 90, '2024-06-18 10:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (62, 31, 7, 11, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-18 10:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (63, 32, 7, 10, 2024, 14, 14, 13, 21, 12, 12, 86, '2024-06-18 10:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (64, 32, 7, 11, 2024, 13, 13, 14, 22, 13, 11, 86, '2024-06-18 10:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (65, 33, 7, 10, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-18 10:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (66, 33, 7, 11, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-18 10:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (67, 34, 7, 10, 2024, 13, 14, 13, 21, 12, 12, 85, '2024-06-18 11:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (68, 34, 7, 11, 2024, 14, 13, 14, 20, 11, 13, 85, '2024-06-18 11:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (69, 35, 7, 10, 2024, 44, 22, 22, NULL, NULL, NULL, 88, '2024-06-18 11:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (70, 35, 7, 11, 2024, 44, 21, 21, NULL, NULL, NULL, 86, '2024-06-18 11:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (71, 36, 8, 11, 2024, 13, 13, 13, 21, 12, 12, 84, '2024-06-18 14:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (72, 36, 8, 12, 2024, 14, 14, 14, 20, 11, 11, 84, '2024-06-18 14:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (73, 37, 8, 11, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-18 14:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (74, 37, 8, 12, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-18 14:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (75, 38, 8, 11, 2024, 14, 13, 13, 21, 12, 12, 85, '2024-06-18 14:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (76, 38, 8, 12, 2024, 13, 14, 14, 22, 12, 11, 86, '2024-06-18 14:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (77, 39, 8, 11, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-18 15:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (78, 39, 8, 12, 2024, 43, 21, 22, NULL, NULL, NULL, 86, '2024-06-18 15:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (79, 40, 8, 11, 2024, 14, 14, 14, 23, 13, 13, 91, '2024-06-18 15:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (80, 40, 8, 12, 2024, 13, 14, 13, 22, 12, 13, 87, '2024-06-18 15:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (81, 41, 9, 13, 2024, 45, 21, 22, NULL, NULL, NULL, 88, '2024-06-19 10:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (82, 41, 9, 14, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2024-06-19 10:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (83, 42, 9, 13, 2024, 14, 13, 13, 21, 12, 12, 85, '2024-06-19 10:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (84, 42, 9, 14, 2024, 13, 14, 14, 22, 11, 11, 85, '2024-06-19 10:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (85, 43, 9, 13, 2024, 44, 21, 21, NULL, NULL, NULL, 86, '2024-06-19 10:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (86, 43, 9, 14, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-19 10:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (87, 44, 9, 13, 2024, 13, 14, 13, 20, 12, 12, 84, '2024-06-19 11:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (88, 44, 9, 14, 2024, 14, 13, 14, 21, 11, 11, 84, '2024-06-19 11:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (89, 45, 9, 13, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-19 11:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (90, 45, 9, 14, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-19 11:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (91, 46, 10, 14, 2024, 13, 13, 13, 21, 12, 12, 84, '2024-06-19 14:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (92, 46, 10, 15, 2024, 14, 14, 14, 20, 11, 11, 84, '2024-06-19 14:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (93, 47, 10, 14, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-19 14:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (94, 47, 10, 15, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-19 14:25:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (95, 48, 10, 14, 2024, 14, 13, 13, 21, 12, 12, 85, '2024-06-19 14:40:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (96, 48, 10, 15, 2024, 13, 14, 14, 22, 11, 12, 86, '2024-06-19 14:45:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (97, 49, 10, 14, 2024, 44, 21, 21, NULL, NULL, NULL, 86, '2024-06-19 15:00:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (98, 49, 10, 15, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-19 15:05:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (99, 50, 10, 14, 2024, 14, 13, 14, 21, 12, 12, 86, '2024-06-19 15:20:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (100, 50, 10, 15, 2024, 13, 14, 13, 22, 13, 11, 86, '2024-06-19 15:25:00', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
                         `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码(加密)',
                         `real_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
                         `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
                         `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
                         `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
                         `role_id` bigint NOT NULL COMMENT '角色ID',
                         `department_id` bigint NULL DEFAULT NULL COMMENT '院系ID',
                         `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         PRIMARY KEY (`id`) USING BTREE,
                         UNIQUE INDEX `username`(`username` ASC) USING BTREE,
                         INDEX `idx_username`(`username` ASC) USING BTREE,
                         INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
                         INDEX `idx_department_id`(`department_id` ASC) USING BTREE,
                         CONSTRAINT `user_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `user_ibfk_2` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 163 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '超级管理员', 'admin@example.com', '13800000000', 1, 1, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (2, 'cs_admin', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '计院管理员', 'cs_admin@example.com', '13800000001', 1, 2, 1, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (3, 'se_admin', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '软院管理员', 'se_admin@example.com', '13800000002', 1, 2, 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (4, 'ice_admin', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '信通管理员', 'ice_admin@example.com', '13800000003', 1, 2, 3, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (5, 'ai_admin', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '人工智能管理员', 'ai_admin@example.com', '13800000004', 1, 2, 4, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (6, 'nsc_admin', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '网安管理员', 'nsc_admin@example.com', '13800000005', 1, 2, 5, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (10, 'T001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '张三教授', 'zhangsan@example.com', '13900010001', 1, 4, 1, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (11, 'T002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '李四副教授', 'lisi@example.com', '13900010002', 1, 4, 1, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (12, 'T003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '王五讲师', 'wangwu@example.com', '13900010003', 1, 4, 1, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (13, 'T004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '赵六教授', 'zhaoliu@example.com', '13900020001', 1, 4, 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (14, 'T005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '钱七副教授', 'qianqi@example.com', '13900020002', 1, 4, 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (15, 'T006', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '孙八讲师', 'sunba@example.com', '13900020003', 1, 4, 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (16, 'T007', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '周九教授', 'zhoujiu@example.com', '13900030001', 1, 4, 3, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (17, 'T008', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '吴十副教授', 'wushi@example.com', '13900030002', 1, 4, 3, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (18, 'T009', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '郑十一讲师', 'zheng11@example.com', '13900030003', 1, 4, 3, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (19, 'T010', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '陈十二教授', 'chen12@example.com', '13900040001', 1, 4, 4, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (20, 'T011', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '林十三副教授', 'lin13@example.com', '13900040002', 1, 4, 4, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (21, 'T012', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '黄十四讲师', 'huang14@example.com', '13900040003', 1, 4, 4, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (22, 'T013', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '刘十五教授', 'liu15@example.com', '13900050001', 1, 4, 5, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (23, 'T014', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '杨十六副教授', 'yang16@example.com', '13900050002', 1, 4, 5, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (24, 'T015', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '徐十七讲师', 'xu17@example.com', '13900050003', 1, 4, 5, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (100, '2021CS001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '张伟', 'zhangwei@stu.edu.cn', '13800001001', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (101, '2021CS002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '李娜', 'lina@stu.edu.cn', '13800001002', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (102, '2021CS003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '王强', 'wangqiang@stu.edu.cn', '13800001003', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (103, '2021CS004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '刘芳', 'liufang@stu.edu.cn', '13800001004', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (104, '2021CS005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '陈明', 'chenming@stu.edu.cn', '13800001005', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (105, '2021CS006', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '杨丽', 'yangli@stu.edu.cn', '13800001006', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (106, '2021CS007', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '赵刚', 'zhaogang@stu.edu.cn', '13800001007', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (107, '2021CS008', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '周洁', 'zhoujie@stu.edu.cn', '13800001008', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (108, '2021CS009', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '吴磊', 'wulei@stu.edu.cn', '13800001009', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (109, '2021CS010', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '郑梅', 'zhengmei@stu.edu.cn', '13800001010', 1, 5, 1, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (110, '2021SE001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '孙浩', 'sunhao@stu.edu.cn', '13800002001', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (111, '2021SE002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '钱红', 'qianhong@stu.edu.cn', '13800002002', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (112, '2021SE003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '周亮', 'zhouliang@stu.edu.cn', '13800002003', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (113, '2021SE004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '吴婷', 'wuting@stu.edu.cn', '13800002004', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (114, '2021SE005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '郑伟', 'zhengwei@stu.edu.cn', '13800002005', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (115, '2021SE006', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '王雪', 'wangxue@stu.edu.cn', '13800002006', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (116, '2021SE007', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '李军', 'lijun@stu.edu.cn', '13800002007', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (117, '2021SE008', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '张艳', 'zhangyan@stu.edu.cn', '13800002008', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (118, '2021SE009', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '刘涛', 'liutao@stu.edu.cn', '13800002009', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (119, '2021SE010', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '陈静', 'chenjing@stu.edu.cn', '13800002010', 1, 5, 2, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (120, '2021ICE001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '黄勇', 'huangyong@stu.edu.cn', '13800003001', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (121, '2021ICE002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '徐丹', 'xudan@stu.edu.cn', '13800003002', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (122, '2021ICE003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '马超', 'machao@stu.edu.cn', '13800003003', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (123, '2021ICE004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '朱敏', 'zhumin@stu.edu.cn', '13800003004', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (124, '2021ICE005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '胡斌', 'hubin@stu.edu.cn', '13800003005', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (125, '2021ICE006', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '郭静', 'guojing@stu.edu.cn', '13800003006', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (126, '2021ICE007', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '林峰', 'linfeng@stu.edu.cn', '13800003007', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (127, '2021ICE008', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '何琳', 'helin@stu.edu.cn', '13800003008', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (128, '2021ICE009', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '罗杰', 'luojie@stu.edu.cn', '13800003009', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (129, '2021ICE010', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '谢芳', 'xiefang@stu.edu.cn', '13800003010', 1, 5, 3, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (130, '2021AI001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '邓雷', 'denglei@stu.edu.cn', '13800004001', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (131, '2021AI002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '韩雪', 'hanxue@stu.edu.cn', '13800004002', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (132, '2021AI003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '曹明', 'caoming@stu.edu.cn', '13800004003', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (133, '2021AI004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '蒋丽', 'jiangli@stu.edu.cn', '13800004004', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (134, '2021AI005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '袁博', 'yuanbo@stu.edu.cn', '13800004005', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (135, '2021AI006', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '唐倩', 'tangqian@stu.edu.cn', '13800004006', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (136, '2021AI007', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '魏刚', 'weigang@stu.edu.cn', '13800004007', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (137, '2021AI008', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '冯婷', 'fengting@stu.edu.cn', '13800004008', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (138, '2021AI009', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '程浩', 'chenghao@stu.edu.cn', '13800004009', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (139, '2021AI010', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '沈琳', 'shenlin@stu.edu.cn', '13800004010', 1, 5, 4, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (140, '2021NSC001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '许强', 'xuqiang@stu.edu.cn', '13800005001', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (141, '2021NSC002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '曾蕾', 'zenglei@stu.edu.cn', '13800005002', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (142, '2021NSC003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '彭涛', 'pengtao@stu.edu.cn', '13800005003', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (143, '2021NSC004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '崔静', 'cuijing@stu.edu.cn', '13800005004', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (144, '2021NSC005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '潘伟', 'panwei@stu.edu.cn', '13800005005', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (145, '2021NSC006', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '范芳', 'fanfang@stu.edu.cn', '13800005006', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (146, '2021NSC007', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '姜明', 'jiangming@stu.edu.cn', '13800005007', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (147, '2021NSC008', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '秦丽', 'qinli@stu.edu.cn', '13800005008', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (148, '2021NSC009', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '夏刚', 'xiagang@stu.edu.cn', '13800005009', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');
INSERT INTO `user` VALUES (149, '2021NSC010', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '田雪', 'tianxue@stu.edu.cn', '13800005010', 1, 5, 5, '2026-02-22 14:10:49', '2026-02-22 14:10:49');

-- =========================================================
-- 补充数据：修正答辩组长角色、增加演示数据
-- =========================================================

-- 1. 修正答辩组长用户的角色为 DEFENSE_LEADER (role_id=3)
--    group_id=1 leader=teacher_id=1 -> user_id=10 -> user.id=10
--    group_id=2 leader=teacher_id=2 -> user_id=11
--    group_id=3 leader=teacher_id=4 -> user_id=13
--    group_id=4 leader=teacher_id=5 -> user_id=14
--    group_id=5 leader=teacher_id=7 -> user_id=16
--    group_id=6 leader=teacher_id=8 -> user_id=17
--    group_id=7 leader=teacher_id=10 -> user_id=19
--    group_id=8 leader=teacher_id=11 -> user_id=20
--    group_id=9 leader=teacher_id=13 -> user_id=22
--    group_id=10 leader=teacher_id=14 -> user_id=23
UPDATE `user` SET `role_id` = 3 WHERE `id` IN (10, 11, 13, 14, 16, 17, 19, 20, 22, 23);

-- 志愿分配演示：CS院学生 1-5 已有导师（流程完整），无需清空

-- 2. 更新截止时间为未来时间（演示用：志愿互选截止在2027年，大组截止在2027年）
UPDATE `system_config` SET `config_value` = '2027-06-30 18:00' WHERE `config_key` = 'VOLUNTEER_DEADLINE';
UPDATE `system_config` SET `config_value` = '2027-07-15 18:00' WHERE `config_key` = 'LARGE_GROUP_DEADLINE';
UPDATE `system_config` SET `config_value` = '0' WHERE `config_key` = 'LARGE_GROUP_ARCHIVED';

-- 3. 为每个小组增加第3位教师的打分记录（小组1-10各加1位老师，共50条记录）
-- 小组1(group_id=1)第3位为teacher_id=3，学生1-5
INSERT INTO `teacher_score_record` VALUES (101, 1, 1, 3, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-15 10:32:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (102, 2, 1, 3, 2024, 13, 14, 14, 21, 12, 12, 86, '2024-06-15 10:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (103, 3, 1, 3, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-15 11:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (104, 4, 1, 3, 2024, 14, 13, 14, 21, 11, 11, 84, '2024-06-15 11:17:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (105, 5, 1, 3, 2024, 45, 23, 22, NULL, NULL, NULL, 90, '2024-06-15 11:32:00', '2025-12-18 12:38:58');
-- 小组2(group_id=2)第3位为teacher_id=1，学生6-10
INSERT INTO `teacher_score_record` VALUES (106, 6, 2, 1, 2024, 14, 13, 14, 22, 12, 11, 86, '2024-06-15 14:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (107, 7, 2, 1, 2024, 45, 22, 23, NULL, NULL, NULL, 90, '2024-06-15 14:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (108, 8, 2, 1, 2024, 13, 14, 13, 22, 12, 12, 86, '2024-06-15 14:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (109, 9, 2, 1, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2024-06-15 15:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (110, 10, 2, 1, 2024, 14, 14, 14, 22, 12, 11, 87, '2024-06-15 15:22:00', '2025-12-18 12:38:58');
-- 小组3(group_id=3)第3位为teacher_id=6，学生11-15
INSERT INTO `teacher_score_record` VALUES (111, 11, 3, 6, 2024, 44, 22, 22, NULL, NULL, NULL, 88, '2024-06-16 10:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (112, 12, 3, 6, 2024, 13, 14, 14, 22, 12, 12, 87, '2024-06-16 10:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (113, 13, 3, 6, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-16 10:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (114, 14, 3, 6, 2024, 14, 13, 13, 21, 12, 12, 85, '2024-06-16 11:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (115, 15, 3, 6, 2024, 44, 22, 22, NULL, NULL, NULL, 88, '2024-06-16 11:22:00', '2025-12-18 12:38:58');
-- 小组4(group_id=4)第3位为teacher_id=4，学生16-20
INSERT INTO `teacher_score_record` VALUES (116, 16, 4, 4, 2024, 13, 14, 14, 22, 12, 11, 86, '2024-06-16 14:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (117, 17, 4, 4, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2024-06-16 14:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (118, 18, 4, 4, 2024, 14, 14, 13, 22, 12, 11, 86, '2024-06-16 14:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (119, 19, 4, 4, 2024, 46, 23, 22, NULL, NULL, NULL, 91, '2024-06-16 15:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (120, 20, 4, 4, 2024, 14, 13, 14, 21, 11, 11, 84, '2024-06-16 15:22:00', '2025-12-18 12:38:58');
-- 小组5(group_id=5)第3位为teacher_id=9，学生21-25
INSERT INTO `teacher_score_record` VALUES (121, 21, 5, 9, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2024-06-17 10:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (122, 22, 5, 9, 2024, 14, 13, 13, 21, 12, 12, 85, '2024-06-17 10:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (123, 23, 5, 9, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-17 10:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (124, 24, 5, 9, 2024, 13, 14, 14, 21, 12, 12, 86, '2024-06-17 11:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (125, 25, 5, 9, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2024-06-17 11:22:00', '2025-12-18 12:38:58');
-- 小组6(group_id=6)第3位为teacher_id=7，学生26-30
INSERT INTO `teacher_score_record` VALUES (126, 26, 6, 7, 2024, 14, 14, 13, 21, 12, 12, 86, '2024-06-17 14:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (127, 27, 6, 7, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-17 14:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (128, 28, 6, 7, 2024, 13, 14, 13, 22, 12, 12, 86, '2024-06-17 14:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (129, 29, 6, 7, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2024-06-17 15:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (130, 30, 6, 7, 2024, 14, 13, 14, 22, 12, 12, 87, '2024-06-17 15:22:00', '2025-12-18 12:38:58');
-- 小组7(group_id=7)第3位为teacher_id=12，学生31-35
INSERT INTO `teacher_score_record` VALUES (131, 31, 7, 12, 2024, 46, 23, 22, NULL, NULL, NULL, 91, '2024-06-18 10:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (132, 32, 7, 12, 2024, 14, 14, 13, 22, 12, 12, 87, '2024-06-18 10:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (133, 33, 7, 12, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-18 10:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (134, 34, 7, 12, 2024, 13, 14, 14, 22, 12, 12, 87, '2024-06-18 11:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (135, 35, 7, 12, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-18 11:22:00', '2025-12-18 12:38:58');
-- 小组8(group_id=8)第3位为teacher_id=10，学生36-40
INSERT INTO `teacher_score_record` VALUES (136, 36, 8, 10, 2024, 13, 14, 14, 22, 12, 11, 86, '2024-06-18 14:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (137, 37, 8, 10, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-18 14:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (138, 38, 8, 10, 2024, 14, 13, 14, 22, 12, 12, 87, '2024-06-18 14:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (139, 39, 8, 10, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-18 15:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (140, 40, 8, 10, 2024, 14, 15, 15, 23, 13, 13, 93, '2024-06-18 15:22:00', '2025-12-18 12:38:58');
-- 小组9(group_id=9)第3位为teacher_id=15，学生41-45
INSERT INTO `teacher_score_record` VALUES (141, 41, 9, 15, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2024-06-19 10:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (142, 42, 9, 15, 2024, 14, 13, 14, 22, 12, 11, 86, '2024-06-19 10:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (143, 43, 9, 15, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2024-06-19 10:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (144, 44, 9, 15, 2024, 13, 14, 13, 21, 12, 12, 85, '2024-06-19 11:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (145, 45, 9, 15, 2024, 45, 23, 22, NULL, NULL, NULL, 90, '2024-06-19 11:22:00', '2025-12-18 12:38:58');
-- 小组10(group_id=10)第3位为teacher_id=13，学生46-50
INSERT INTO `teacher_score_record` VALUES (146, 46, 10, 13, 2024, 14, 13, 13, 22, 12, 12, 86, '2024-06-19 14:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (147, 47, 10, 13, 2024, 45, 22, 23, NULL, NULL, NULL, 90, '2024-06-19 14:22:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (148, 48, 10, 13, 2024, 13, 14, 14, 22, 12, 11, 86, '2024-06-19 14:42:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (149, 49, 10, 13, 2024, 44, 22, 22, NULL, NULL, NULL, 88, '2024-06-19 15:02:00', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (150, 50, 10, 13, 2024, 14, 14, 14, 22, 13, 12, 89, '2024-06-19 15:22:00', '2025-12-18 12:38:58');

-- 4. 补充所有院系已完成答辩学生的志愿记录（status=1 表示已处理/已录取）
-- 计算机院 CS（学生 1-10，导师 T001-T003）
INSERT INTO `student_preference` VALUES (11, 1, 2024, 1, 2, 3, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 09:00:00', '2024-03-10 09:00:00');
INSERT INTO `student_preference` VALUES (12, 2, 2024, 1, 2, 3, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 09:10:00', '2024-03-10 09:10:00');
INSERT INTO `student_preference` VALUES (13, 3, 2024, 2, 1, 3, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 09:20:00', '2024-03-10 09:20:00');
INSERT INTO `student_preference` VALUES (14, 4, 2024, 2, 3, 1, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 09:30:00', '2024-03-10 09:30:00');
INSERT INTO `student_preference` VALUES (15, 5, 2024, 3, 1, 2, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 09:40:00', '2024-03-10 09:40:00');
INSERT INTO `student_preference` VALUES (16, 6, 2024, 3, 2, 1, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 09:50:00', '2024-03-10 09:50:00');
INSERT INTO `student_preference` VALUES (17, 7, 2024, 1, 3, 2, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 10:00:00', '2024-03-10 10:00:00');
INSERT INTO `student_preference` VALUES (18, 8, 2024, 2, 1, 3, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 10:10:00', '2024-03-10 10:10:00');
INSERT INTO `student_preference` VALUES (19, 9, 2024, 3, 2, 1, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 10:20:00', '2024-03-10 10:20:00');
INSERT INTO `student_preference` VALUES (20, 10, 2024, 1, 2, 3, NULL, NULL, NULL, 1, 0, NULL, '2024-03-10 10:30:00', '2024-03-10 10:30:00');
-- 软件院 SE（学生 11-20，导师 T004-T006）
INSERT INTO `student_preference` VALUES (21, 11, 2024, 4, 5, 6, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 09:00:00', '2024-03-11 09:00:00');
INSERT INTO `student_preference` VALUES (22, 12, 2024, 4, 6, 5, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 09:10:00', '2024-03-11 09:10:00');
INSERT INTO `student_preference` VALUES (23, 13, 2024, 5, 4, 6, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 09:20:00', '2024-03-11 09:20:00');
INSERT INTO `student_preference` VALUES (24, 14, 2024, 5, 6, 4, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 09:30:00', '2024-03-11 09:30:00');
INSERT INTO `student_preference` VALUES (25, 15, 2024, 6, 4, 5, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 09:40:00', '2024-03-11 09:40:00');
INSERT INTO `student_preference` VALUES (26, 16, 2024, 6, 5, 4, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 09:50:00', '2024-03-11 09:50:00');
INSERT INTO `student_preference` VALUES (27, 17, 2024, 4, 5, 6, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 10:00:00', '2024-03-11 10:00:00');
INSERT INTO `student_preference` VALUES (28, 18, 2024, 5, 4, 6, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 10:10:00', '2024-03-11 10:10:00');
INSERT INTO `student_preference` VALUES (29, 19, 2024, 6, 5, 4, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 10:20:00', '2024-03-11 10:20:00');
INSERT INTO `student_preference` VALUES (30, 20, 2024, 4, 6, 5, NULL, NULL, NULL, 1, 0, NULL, '2024-03-11 10:30:00', '2024-03-11 10:30:00');
-- 信通院 ICE（学生 21-30，导师 T007-T009）
INSERT INTO `student_preference` VALUES (31, 21, 2024, 7, 8, 9, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 09:00:00', '2024-03-12 09:00:00');
INSERT INTO `student_preference` VALUES (32, 22, 2024, 7, 9, 8, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 09:10:00', '2024-03-12 09:10:00');
INSERT INTO `student_preference` VALUES (33, 23, 2024, 8, 7, 9, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 09:20:00', '2024-03-12 09:20:00');
INSERT INTO `student_preference` VALUES (34, 24, 2024, 8, 9, 7, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 09:30:00', '2024-03-12 09:30:00');
INSERT INTO `student_preference` VALUES (35, 25, 2024, 9, 7, 8, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 09:40:00', '2024-03-12 09:40:00');
INSERT INTO `student_preference` VALUES (36, 26, 2024, 9, 8, 7, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 09:50:00', '2024-03-12 09:50:00');
INSERT INTO `student_preference` VALUES (37, 27, 2024, 7, 8, 9, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 10:00:00', '2024-03-12 10:00:00');
INSERT INTO `student_preference` VALUES (38, 28, 2024, 8, 7, 9, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 10:10:00', '2024-03-12 10:10:00');
INSERT INTO `student_preference` VALUES (39, 29, 2024, 9, 8, 7, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 10:20:00', '2024-03-12 10:20:00');
INSERT INTO `student_preference` VALUES (40, 30, 2024, 7, 9, 8, NULL, NULL, NULL, 1, 0, NULL, '2024-03-12 10:30:00', '2024-03-12 10:30:00');
-- AI学院（学生 31-40，导师 T010-T012）
INSERT INTO `student_preference` VALUES (41, 31, 2024, 10, 11, 12, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 09:00:00', '2024-03-13 09:00:00');
INSERT INTO `student_preference` VALUES (42, 32, 2024, 10, 12, 11, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 09:10:00', '2024-03-13 09:10:00');
INSERT INTO `student_preference` VALUES (43, 33, 2024, 11, 10, 12, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 09:20:00', '2024-03-13 09:20:00');
INSERT INTO `student_preference` VALUES (44, 34, 2024, 11, 12, 10, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 09:30:00', '2024-03-13 09:30:00');
INSERT INTO `student_preference` VALUES (45, 35, 2024, 12, 10, 11, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 09:40:00', '2024-03-13 09:40:00');
INSERT INTO `student_preference` VALUES (46, 36, 2024, 12, 11, 10, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 09:50:00', '2024-03-13 09:50:00');
INSERT INTO `student_preference` VALUES (47, 37, 2024, 10, 11, 12, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 10:00:00', '2024-03-13 10:00:00');
INSERT INTO `student_preference` VALUES (48, 38, 2024, 11, 10, 12, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 10:10:00', '2024-03-13 10:10:00');
INSERT INTO `student_preference` VALUES (49, 39, 2024, 12, 11, 10, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 10:20:00', '2024-03-13 10:20:00');
INSERT INTO `student_preference` VALUES (50, 40, 2024, 10, 12, 11, NULL, NULL, NULL, 1, 0, NULL, '2024-03-13 10:30:00', '2024-03-13 10:30:00');
-- 网安院 NSC（学生 41-50，导师 T013-T015）已在原始数据中，更新 status=1
UPDATE `student_preference` SET `status` = 1 WHERE `id` BETWEEN 1 AND 10;

-- 5. 为大组答辩增加第三位教师的评分（3 teachers per大组答辩student）
INSERT INTO `large_group_score` VALUES (23, 1, 3, 2024, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (24, 5, 2, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (25, 7, 1, 2024, 92, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (26, 11, 6, 2024, 89, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (27, 19, 5, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (28, 21, 9, 2024, 88, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (29, 27, 8, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (30, 31, 12, 2024, 91, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (31, 40, 11, 2024, 92, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (32, 41, 15, 2024, 88, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (33, 47, 14, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- =========================================================
-- 新增 25 名"进行中"学生（id 51-75，2024届，有导师有志愿，答辩打分尚未完成，无最终成绩）
-- 每院系 5 人，分配到现有答辩小组，体现系统"进行中"状态
-- =========================================================

-- t_student：新增 25 名学生
-- 计算机院（dept=1，导师 T001-T003，小组 1-2）
INSERT INTO `t_student` VALUES (51, '2022CS001', '方浩', '计科2201', 1, 1, 2, 'PAPER', '基于大语言模型的代码补全研究', '探索LLM在智能编程辅助中的应用。', 1, 2024, '13800011001', 'fanghao@stu.edu.cn', NULL, NULL, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `t_student` VALUES (52, '2022CS002', '苏婷', '计科2201', 1, 2, 3, 'DESIGN', '基于知识蒸馏的轻量化模型设计', '研究模型压缩在边缘计算中的应用。', 1, 2024, '13800011002', 'suting@stu.edu.cn', NULL, NULL, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `t_student` VALUES (53, '2022CS003', '卢鹏', '计科2202', 1, 3, 1, 'PAPER', '容器编排与微服务弹性伸缩策略', '研究Kubernetes在生产环境中的调度优化。', 2, 2024, '13800011003', 'lupeng@stu.edu.cn', NULL, NULL, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `t_student` VALUES (54, '2022CS004', '贺敏', '计科2202', 1, 1, 3, 'DESIGN', '基于图数据库的社交关系分析系统', '使用Neo4j构建社交网络分析平台。', 2, 2024, '13800011004', 'hemin@stu.edu.cn', NULL, NULL, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `t_student` VALUES (55, '2022CS005', '龚磊', '计科2203', 1, 2, 1, 'PAPER', 'WebAssembly在高性能Web应用中的研究', '探索WASM在浏览器端计算密集型任务的应用。', 2, 2024, '13800011005', 'gonglei@stu.edu.cn', NULL, NULL, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
-- 软件院（dept=2，导师 T004-T006，小组 3-4）
INSERT INTO `t_student` VALUES (56, '2022SE001', '石雨', '软工2201', 2, 4, 5, 'PAPER', '基于静态分析的安全漏洞检测框架', '研究代码静态分析在安全审计中的应用。', 3, 2024, '13800012001', 'shiyu@stu.edu.cn', NULL, NULL, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `t_student` VALUES (57, '2022SE002', '廖杰', '软工2201', 2, 5, 6, 'DESIGN', '基于领域驱动设计的电商平台重构', '应用DDD思想对遗留系统进行现代化改造。', 3, 2024, '13800012002', 'liaojie@stu.edu.cn', NULL, NULL, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `t_student` VALUES (58, '2022SE003', '熊芳', '软工2202', 2, 6, 4, 'PAPER', '基于事件溯源的分布式事务一致性研究', '探索Event Sourcing在微服务中的实践。', 4, 2024, '13800012003', 'xiongfang@stu.edu.cn', NULL, NULL, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `t_student` VALUES (59, '2022SE004', '严浩', '软工2202', 2, 4, 6, 'DESIGN', '基于低代码平台的工作流引擎设计', '研究可视化工作流在企业应用中的实现。', 4, 2024, '13800012004', 'yanhao@stu.edu.cn', NULL, NULL, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `t_student` VALUES (60, '2022SE005', '傅丽', '软工2203', 2, 5, 4, 'PAPER', '基于行为驱动开发的测试自动化研究', '研究BDD在敏捷团队中的落地实践。', 4, 2024, '13800012005', 'fuli@stu.edu.cn', NULL, NULL, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
-- 信通院（dept=3，导师 T007-T009，小组 5-6）
INSERT INTO `t_student` VALUES (61, '2022ICE001', '汪涛', '信通2201', 3, 7, 8, 'PAPER', '基于深度学习的无线信道预测', '研究LSTM在时变信道建模中的应用。', 5, 2024, '13800013001', 'wangtao@stu.edu.cn', NULL, NULL, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `t_student` VALUES (62, '2022ICE002', '孟雪', '信通2201', 3, 8, 9, 'DESIGN', '基于FPGA的实时信号处理系统', '设计并实现高速数字信号处理硬件平台。', 5, 2024, '13800013002', 'mengxue@stu.edu.cn', NULL, NULL, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `t_student` VALUES (63, '2022ICE003', '尹强', '信通2202', 3, 9, 7, 'PAPER', '毫米波通信波束管理算法研究', '研究5G毫米波场景下的波束追踪技术。', 6, 2024, '13800013003', 'yinqiang@stu.edu.cn', NULL, NULL, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `t_student` VALUES (64, '2022ICE004', '乔静', '信通2202', 3, 7, 9, 'DESIGN', '基于NB-IoT的智慧农业监测系统', '设计低功耗广域网农业传感器网络。', 6, 2024, '13800013004', 'qiaojing@stu.edu.cn', NULL, NULL, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `t_student` VALUES (65, '2022ICE005', '邵明', '信通2203', 3, 8, 7, 'PAPER', '认知无线电网络中的动态频谱接入', '研究基于强化学习的频谱接入策略。', 6, 2024, '13800013005', 'shaoming@stu.edu.cn', NULL, NULL, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
-- AI学院（dept=4，导师 T010-T012，小组 7-8）
INSERT INTO `t_student` VALUES (66, '2022AI001', '柴鹏', 'AI2201', 4, 10, 11, 'PAPER', '基于扩散模型的图像生成研究', '探索Diffusion Model在高质量图像合成中的应用。', 7, 2024, '13800014001', 'chaipeng@stu.edu.cn', NULL, NULL, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `t_student` VALUES (67, '2022AI002', '阮婷', 'AI2201', 4, 11, 12, 'DESIGN', '基于多模态学习的医疗影像诊断系统', '研究图文融合在医学图像分析中的应用。', 7, 2024, '13800014002', 'ruanting@stu.edu.cn', NULL, NULL, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `t_student` VALUES (68, '2022AI003', '侯磊', 'AI2202', 4, 12, 10, 'PAPER', '基于对比学习的自监督表示学习', '研究无标签数据在视觉预训练中的利用。', 8, 2024, '13800014003', 'houlei@stu.edu.cn', NULL, NULL, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `t_student` VALUES (69, '2022AI004', '龙丽', 'AI2202', 4, 10, 12, 'DESIGN', '基于Transformer的时序预测系统', '研究注意力机制在金融时序数据预测中的应用。', 8, 2024, '13800014004', 'longli@stu.edu.cn', NULL, NULL, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `t_student` VALUES (70, '2022AI005', '谭浩', 'AI2203', 4, 11, 10, 'PAPER', '基于元学习的小样本分类研究', '探索Few-Shot Learning在工业缺陷检测中的应用。', 8, 2024, '13800014005', 'tanhao@stu.edu.cn', NULL, NULL, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
-- 网安院（dept=5，导师 T013-T015，小组 9-10）
INSERT INTO `t_student` VALUES (71, '2022NSC001', '蔡强', '网安2201', 5, 13, 14, 'PAPER', '基于机器学习的APT攻击检测', '研究高级持续性威胁的行为特征识别。', 9, 2024, '13800015001', 'caiqiang@stu.edu.cn', NULL, NULL, '2026-01-14 09:00:00', '2026-01-14 09:00:00');
INSERT INTO `t_student` VALUES (72, '2022NSC002', '贾雪', '网安2201', 5, 14, 15, 'DESIGN', '基于区块链的数字身份认证系统', '设计去中心化的可信身份管理平台。', 9, 2024, '13800015002', 'jiaxue@stu.edu.cn', NULL, NULL, '2026-01-14 09:00:00', '2026-01-14 09:00:00');
INSERT INTO `t_student` VALUES (73, '2022NSC003', '丁磊', '网安2202', 5, 15, 13, 'PAPER', '基于深度学习的网络流量异常检测', '研究自编码器在网络入侵检测中的应用。', 10, 2024, '13800015003', 'dinglei@stu.edu.cn', NULL, NULL, '2026-01-14 09:00:00', '2026-01-14 09:00:00');
INSERT INTO `t_student` VALUES (74, '2022NSC004', '薛婷', '网安2202', 5, 13, 15, 'DESIGN', '基于同态加密的隐私计算平台', '设计支持密文计算的数据安全共享系统。', 10, 2024, '13800015004', 'xueting@stu.edu.cn', NULL, NULL, '2026-01-14 09:00:00', '2026-01-14 09:00:00');
INSERT INTO `t_student` VALUES (75, '2022NSC005', '冉浩', '网安2203', 5, 14, 13, 'PAPER', '软件供应链安全风险分析与防护', '研究开源组件漏洞传播与依赖风险管理。', 10, 2024, '13800015005', 'ranhao@stu.edu.cn', NULL, NULL, '2026-01-14 09:00:00', '2026-01-14 09:00:00');

-- user 账号：新增 25 名学生登录账号（id 150-174，密码同为 123456 的 bcrypt）
INSERT INTO `user` VALUES (150, '2022CS001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '方浩', 'fanghao@stu.edu.cn', '13800011001', 1, 5, 1, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `user` VALUES (151, '2022CS002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '苏婷', 'suting@stu.edu.cn', '13800011002', 1, 5, 1, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `user` VALUES (152, '2022CS003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '卢鹏', 'lupeng@stu.edu.cn', '13800011003', 1, 5, 1, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `user` VALUES (153, '2022CS004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '贺敏', 'hemin@stu.edu.cn', '13800011004', 1, 5, 1, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `user` VALUES (154, '2022CS005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '龚磊', 'gonglei@stu.edu.cn', '13800011005', 1, 5, 1, '2026-01-10 09:00:00', '2026-01-10 09:00:00');
INSERT INTO `user` VALUES (155, '2022SE001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '石雨', 'shiyu@stu.edu.cn', '13800012001', 1, 5, 2, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `user` VALUES (156, '2022SE002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '廖杰', 'liaojie@stu.edu.cn', '13800012002', 1, 5, 2, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `user` VALUES (157, '2022SE003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '熊芳', 'xiongfang@stu.edu.cn', '13800012003', 1, 5, 2, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `user` VALUES (158, '2022SE004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '严浩', 'yanhao@stu.edu.cn', '13800012004', 1, 5, 2, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `user` VALUES (159, '2022SE005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '傅丽', 'fuli@stu.edu.cn', '13800012005', 1, 5, 2, '2026-01-11 09:00:00', '2026-01-11 09:00:00');
INSERT INTO `user` VALUES (160, '2022ICE001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '汪涛', 'wangtao@stu.edu.cn', '13800013001', 1, 5, 3, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `user` VALUES (161, '2022ICE002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '孟雪', 'mengxue@stu.edu.cn', '13800013002', 1, 5, 3, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `user` VALUES (162, '2022ICE003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '尹强', 'yinqiang@stu.edu.cn', '13800013003', 1, 5, 3, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `user` VALUES (163, '2022ICE004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '乔静', 'qiaojing@stu.edu.cn', '13800013004', 1, 5, 3, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `user` VALUES (164, '2022ICE005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '邵明', 'shaoming@stu.edu.cn', '13800013005', 1, 5, 3, '2026-01-12 09:00:00', '2026-01-12 09:00:00');
INSERT INTO `user` VALUES (165, '2022AI001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '柴鹏', 'chaipeng@stu.edu.cn', '13800014001', 1, 5, 4, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `user` VALUES (166, '2022AI002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '阮婷', 'ruanting@stu.edu.cn', '13800014002', 1, 5, 4, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `user` VALUES (167, '2022AI003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '侯磊', 'houlei@stu.edu.cn', '13800014003', 1, 5, 4, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `user` VALUES (168, '2022AI004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '龙丽', 'longli@stu.edu.cn', '13800014004', 1, 5, 4, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `user` VALUES (169, '2022AI005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '谭浩', 'tanhao@stu.edu.cn', '13800014005', 1, 5, 4, '2026-01-13 09:00:00', '2026-01-13 09:00:00');
INSERT INTO `user` VALUES (170, '2022NSC001', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '蔡强', 'caiqiang@stu.edu.cn', '13800015001', 1, 5, 5, '2026-01-14 09:00:00', '2026-01-14 09:00:00');
INSERT INTO `user` VALUES (171, '2022NSC002', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '贾雪', 'jiaxue@stu.edu.cn', '13800015002', 1, 5, 5, '2026-01-14 09:00:00', '2026-01-14 09:00:00');
INSERT INTO `user` VALUES (172, '2022NSC003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '丁磊', 'dinglei@stu.edu.cn', '13800015003', 1, 5, 5, '2026-01-14 09:00:00', '2026-01-14 09:00:00');
INSERT INTO `user` VALUES (173, '2022NSC004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '薛婷', 'xueting@stu.edu.cn', '13800015004', 1, 5, 5, '2026-01-14 09:00:00', '2026-01-14 09:00:00');
INSERT INTO `user` VALUES (174, '2022NSC005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '冉浩', 'ranhao@stu.edu.cn', '13800015005', 1, 5, 5, '2026-01-14 09:00:00', '2026-01-14 09:00:00');

-- student_preference：新增 25 名学生的志愿记录（status=1 已录取，导师已确认）
-- CS 院（志愿指向本院教师 T001-T003，id 1-3）
INSERT INTO `student_preference` VALUES (51, 51, 2024, 1, 2, 3, NULL, NULL, NULL, 1, 0, NULL, '2026-01-15 10:00:00', '2026-01-15 10:00:00');
INSERT INTO `student_preference` VALUES (52, 52, 2024, 2, 1, 3, NULL, NULL, NULL, 1, 0, NULL, '2026-01-15 10:05:00', '2026-01-15 10:05:00');
INSERT INTO `student_preference` VALUES (53, 53, 2024, 3, 2, 1, NULL, NULL, NULL, 1, 0, NULL, '2026-01-15 10:10:00', '2026-01-15 10:10:00');
INSERT INTO `student_preference` VALUES (54, 54, 2024, 1, 3, 2, NULL, NULL, NULL, 1, 0, NULL, '2026-01-15 10:15:00', '2026-01-15 10:15:00');
INSERT INTO `student_preference` VALUES (55, 55, 2024, 2, 3, 1, NULL, NULL, NULL, 1, 0, NULL, '2026-01-15 10:20:00', '2026-01-15 10:20:00');
-- SE 院（T004-T006，id 4-6）
INSERT INTO `student_preference` VALUES (56, 56, 2024, 4, 5, 6, NULL, NULL, NULL, 1, 0, NULL, '2026-01-16 10:00:00', '2026-01-16 10:00:00');
INSERT INTO `student_preference` VALUES (57, 57, 2024, 5, 4, 6, NULL, NULL, NULL, 1, 0, NULL, '2026-01-16 10:05:00', '2026-01-16 10:05:00');
INSERT INTO `student_preference` VALUES (58, 58, 2024, 6, 4, 5, NULL, NULL, NULL, 1, 0, NULL, '2026-01-16 10:10:00', '2026-01-16 10:10:00');
INSERT INTO `student_preference` VALUES (59, 59, 2024, 4, 6, 5, NULL, NULL, NULL, 1, 0, NULL, '2026-01-16 10:15:00', '2026-01-16 10:15:00');
INSERT INTO `student_preference` VALUES (60, 60, 2024, 5, 6, 4, NULL, NULL, NULL, 1, 0, NULL, '2026-01-16 10:20:00', '2026-01-16 10:20:00');
-- ICE 院（T007-T009，id 7-9）
INSERT INTO `student_preference` VALUES (61, 61, 2024, 7, 8, 9, NULL, NULL, NULL, 1, 0, NULL, '2026-01-17 10:00:00', '2026-01-17 10:00:00');
INSERT INTO `student_preference` VALUES (62, 62, 2024, 8, 7, 9, NULL, NULL, NULL, 1, 0, NULL, '2026-01-17 10:05:00', '2026-01-17 10:05:00');
INSERT INTO `student_preference` VALUES (63, 63, 2024, 9, 7, 8, NULL, NULL, NULL, 1, 0, NULL, '2026-01-17 10:10:00', '2026-01-17 10:10:00');
INSERT INTO `student_preference` VALUES (64, 64, 2024, 7, 9, 8, NULL, NULL, NULL, 1, 0, NULL, '2026-01-17 10:15:00', '2026-01-17 10:15:00');
INSERT INTO `student_preference` VALUES (65, 65, 2024, 8, 9, 7, NULL, NULL, NULL, 1, 0, NULL, '2026-01-17 10:20:00', '2026-01-17 10:20:00');
-- AI 院（T010-T012，id 10-12）
INSERT INTO `student_preference` VALUES (66, 66, 2024, 10, 11, 12, NULL, NULL, NULL, 1, 0, NULL, '2026-01-18 10:00:00', '2026-01-18 10:00:00');
INSERT INTO `student_preference` VALUES (67, 67, 2024, 11, 10, 12, NULL, NULL, NULL, 1, 0, NULL, '2026-01-18 10:05:00', '2026-01-18 10:05:00');
INSERT INTO `student_preference` VALUES (68, 68, 2024, 12, 10, 11, NULL, NULL, NULL, 1, 0, NULL, '2026-01-18 10:10:00', '2026-01-18 10:10:00');
INSERT INTO `student_preference` VALUES (69, 69, 2024, 10, 12, 11, NULL, NULL, NULL, 1, 0, NULL, '2026-01-18 10:15:00', '2026-01-18 10:15:00');
INSERT INTO `student_preference` VALUES (70, 70, 2024, 11, 12, 10, NULL, NULL, NULL, 1, 0, NULL, '2026-01-18 10:20:00', '2026-01-18 10:20:00');
-- NSC 院（T013-T015，id 13-15）
INSERT INTO `student_preference` VALUES (71, 71, 2024, 13, 14, 15, NULL, NULL, NULL, 1, 0, NULL, '2026-01-19 10:00:00', '2026-01-19 10:00:00');
INSERT INTO `student_preference` VALUES (72, 72, 2024, 14, 13, 15, NULL, NULL, NULL, 1, 0, NULL, '2026-01-19 10:05:00', '2026-01-19 10:05:00');
INSERT INTO `student_preference` VALUES (73, 73, 2024, 15, 13, 14, NULL, NULL, NULL, 1, 0, NULL, '2026-01-19 10:10:00', '2026-01-19 10:10:00');
INSERT INTO `student_preference` VALUES (74, 74, 2024, 13, 15, 14, NULL, NULL, NULL, 1, 0, NULL, '2026-01-19 10:15:00', '2026-01-19 10:15:00');
INSERT INTO `student_preference` VALUES (75, 75, 2024, 14, 15, 13, NULL, NULL, NULL, 1, 0, NULL, '2026-01-19 10:20:00', '2026-01-19 10:20:00');

-- teacher_score_record：新增 25 名学生的"进行中"打分（每人只有 1 位教师打了分，体现答辩尚未完成）
-- CS 院小组 1（group_id=1，teacher_id=1 已打分，teacher_id=2 未打）
INSERT INTO `teacher_score_record` VALUES (151, 51, 1, 1, 2024, 46, 22, 22, NULL, NULL, NULL, 90, '2026-03-20 10:00:00', '2026-03-20 10:00:00');
INSERT INTO `teacher_score_record` VALUES (152, 52, 1, 1, 2024, 13, 14, 14, 22, 12, 12, 87, '2026-03-20 10:10:00', '2026-03-20 10:10:00');
-- CS 院小组 2（group_id=2，teacher_id=2 已打分）
INSERT INTO `teacher_score_record` VALUES (153, 53, 2, 2, 2024, 44, 22, 22, NULL, NULL, NULL, 88, '2026-03-20 14:00:00', '2026-03-20 14:00:00');
INSERT INTO `teacher_score_record` VALUES (154, 54, 2, 2, 2024, 14, 13, 14, 22, 12, 11, 86, '2026-03-20 14:10:00', '2026-03-20 14:10:00');
INSERT INTO `teacher_score_record` VALUES (155, 55, 2, 2, 2024, 45, 22, 21, NULL, NULL, NULL, 88, '2026-03-20 14:20:00', '2026-03-20 14:20:00');
-- SE 院小组 3（group_id=3，teacher_id=4 已打分）
INSERT INTO `teacher_score_record` VALUES (156, 56, 3, 4, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2026-03-21 10:00:00', '2026-03-21 10:00:00');
INSERT INTO `teacher_score_record` VALUES (157, 57, 3, 4, 2024, 14, 14, 13, 22, 12, 12, 87, '2026-03-21 10:10:00', '2026-03-21 10:10:00');
-- SE 院小组 4（group_id=4，teacher_id=5 已打分）
INSERT INTO `teacher_score_record` VALUES (158, 58, 4, 5, 2024, 44, 21, 22, NULL, NULL, NULL, 87, '2026-03-21 14:00:00', '2026-03-21 14:00:00');
INSERT INTO `teacher_score_record` VALUES (159, 59, 4, 5, 2024, 13, 14, 14, 22, 12, 12, 87, '2026-03-21 14:10:00', '2026-03-21 14:10:00');
INSERT INTO `teacher_score_record` VALUES (160, 60, 4, 5, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2026-03-21 14:20:00', '2026-03-21 14:20:00');
-- ICE 院小组 5（group_id=5，teacher_id=7 已打分）
INSERT INTO `teacher_score_record` VALUES (161, 61, 5, 7, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2026-03-22 10:00:00', '2026-03-22 10:00:00');
INSERT INTO `teacher_score_record` VALUES (162, 62, 5, 7, 2024, 14, 13, 13, 21, 12, 12, 85, '2026-03-22 10:10:00', '2026-03-22 10:10:00');
-- ICE 院小组 6（group_id=6，teacher_id=8 已打分）
INSERT INTO `teacher_score_record` VALUES (163, 63, 6, 8, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2026-03-22 14:00:00', '2026-03-22 14:00:00');
INSERT INTO `teacher_score_record` VALUES (164, 64, 6, 8, 2024, 13, 14, 14, 22, 12, 11, 86, '2026-03-22 14:10:00', '2026-03-22 14:10:00');
INSERT INTO `teacher_score_record` VALUES (165, 65, 6, 8, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2026-03-22 14:20:00', '2026-03-22 14:20:00');
-- AI 院小组 7（group_id=7，teacher_id=10 已打分）
INSERT INTO `teacher_score_record` VALUES (166, 66, 7, 10, 2024, 46, 23, 22, NULL, NULL, NULL, 91, '2026-03-23 10:00:00', '2026-03-23 10:00:00');
INSERT INTO `teacher_score_record` VALUES (167, 67, 7, 10, 2024, 14, 14, 13, 22, 12, 12, 87, '2026-03-23 10:10:00', '2026-03-23 10:10:00');
-- AI 院小组 8（group_id=8，teacher_id=11 已打分）
INSERT INTO `teacher_score_record` VALUES (168, 68, 8, 11, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2026-03-23 14:00:00', '2026-03-23 14:00:00');
INSERT INTO `teacher_score_record` VALUES (169, 69, 8, 11, 2024, 14, 13, 14, 22, 12, 12, 87, '2026-03-23 14:10:00', '2026-03-23 14:10:00');
INSERT INTO `teacher_score_record` VALUES (170, 70, 8, 11, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2026-03-23 14:20:00', '2026-03-23 14:20:00');
-- NSC 院小组 9（group_id=9，teacher_id=13 已打分）
INSERT INTO `teacher_score_record` VALUES (171, 71, 9, 13, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2026-03-24 10:00:00', '2026-03-24 10:00:00');
INSERT INTO `teacher_score_record` VALUES (172, 72, 9, 13, 2024, 13, 14, 14, 22, 12, 11, 86, '2026-03-24 10:10:00', '2026-03-24 10:10:00');
-- NSC 院小组 10（group_id=10，teacher_id=14 已打分）
INSERT INTO `teacher_score_record` VALUES (173, 73, 10, 14, 2024, 44, 22, 21, NULL, NULL, NULL, 87, '2026-03-24 14:00:00', '2026-03-24 14:00:00');
INSERT INTO `teacher_score_record` VALUES (174, 74, 10, 14, 2024, 14, 13, 13, 21, 12, 12, 85, '2026-03-24 14:10:00', '2026-03-24 14:10:00');
INSERT INTO `teacher_score_record` VALUES (175, 75, 10, 14, 2024, 45, 22, 22, NULL, NULL, NULL, 89, '2026-03-24 14:20:00', '2026-03-24 14:20:00');

SET FOREIGN_KEY_CHECKS = 1;

