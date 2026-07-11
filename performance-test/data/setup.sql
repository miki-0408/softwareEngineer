-- ============================================
-- Netdisk 性能测试 — 测试数据初始化脚本
-- 运行前执行: mysql -u root -p netdisk < setup.sql
-- ============================================

-- 1. 创建 10 个测试用户
INSERT IGNORE INTO `user` (name, password, sex, role)
SELECT CONCAT('perf_test_', n), '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQkfAjkMBcGmBmP2FmUFxYzGkN5N2i', '未知', 'user'
FROM (
  SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) numbers
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE name LIKE 'perf_test_%');

-- 2. 为每个测试用户创建存储空间
INSERT IGNORE INTO `storage_space` (userId, totalSpace, usedSpace, remainSpace)
SELECT userId, 10737418240, 0, 10737418240 FROM `user` WHERE name LIKE 'perf_test_%'
AND NOT EXISTS (SELECT 1 FROM `storage_space` s WHERE s.userId = `user`.userId);

-- 3. 为每个测试用户创建根目录 "我的文件"
INSERT IGNORE INTO `directory` (dirName, parentDirId, userId)
SELECT '我的文件', NULL, userId FROM `user` WHERE name LIKE 'perf_test_%'
AND NOT EXISTS (SELECT 1 FROM `directory` d WHERE d.userId = `user`.userId AND d.dirName = '我的文件');

-- 4. 为每个用户创建私密空间记录（密码 test123 的 bcrypt hash）
INSERT IGNORE INTO `private_space` (userId, password, isEncrypted)
SELECT userId, '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQkfAjkMBcGmBmP2FmUFxYzGkN5N2i', 1
FROM `user` WHERE name LIKE 'perf_test_%'
AND NOT EXISTS (SELECT 1 FROM `private_space` p WHERE p.userId = `user`.userId);
