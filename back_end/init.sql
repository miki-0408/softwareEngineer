USE netdisk;

-- 1. 用户表 (User)
CREATE TABLE IF NOT EXISTS `user` (
    `userId` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '姓名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `sex` VARCHAR(10) DEFAULT '未知' COMMENT '性别',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(50) DEFAULT 'user' COMMENT '角色'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 目录表 (Directory)
CREATE TABLE IF NOT EXISTS `directory` (
    `dirId` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '目录ID',
    `dirName` VARCHAR(255) NOT NULL COMMENT '目录名称',
    `parentDirId` BIGINT DEFAULT NULL COMMENT '父目录ID (为NULL表示根目录)',
    `userId` BIGINT NOT NULL COMMENT '所属用户ID',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`userId`) REFERENCES `user`(`userId`) ON DELETE CASCADE,
    FOREIGN KEY (`parentDirId`) REFERENCES `directory`(`dirId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目录表';

-- 3. 文件表 (File)
CREATE TABLE IF NOT EXISTS `file` (
    `fileId` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件ID',
    `fileName` VARCHAR(255) NOT NULL COMMENT '文件名',
    `fileType` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
    `fileSize` BIGINT DEFAULT 0 COMMENT '文件大小 (字节，存储压缩后大小)',
    `path` VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    `uploadTime` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `userId` BIGINT NOT NULL COMMENT '所属用户ID',
    `dirId` BIGINT DEFAULT NULL COMMENT '所属目录ID',
    `isEncrypted` TINYINT DEFAULT 0 COMMENT '是否加密 (0=否, 1=是)',
    `status` TINYINT DEFAULT 0 COMMENT '文件状态 (0=正常,1=已删除)',
    `compressMethod` TINYINT DEFAULT 1 COMMENT '压缩方式: 1=LZ77, 2=Huffman',
    FOREIGN KEY (`userId`) REFERENCES `user`(`userId`) ON DELETE CASCADE,
    FOREIGN KEY (`dirId`) REFERENCES `directory`(`dirId`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 4. 存储空间表 (Storage Space)
CREATE TABLE IF NOT EXISTS `storage_space` (
    `userId` BIGINT PRIMARY KEY COMMENT '用户ID (主键，关联用户表)',
    `totalSpace` BIGINT DEFAULT 10737418240 COMMENT '总容量 (默认10GB，单位字节)',
    `usedSpace` BIGINT DEFAULT 0 COMMENT '已用容量 (单位字节)',
    `remainSpace` BIGINT DEFAULT 10737418240 COMMENT '剩余容量 (单位字节)',
    FOREIGN KEY (`userId`) REFERENCES `user`(`userId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户存储空间表';

-- 5. 日志表 (Log)
CREATE TABLE IF NOT EXISTS `log` (
    `logId` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `operatorId` BIGINT NOT NULL COMMENT '操作人ID',
    `time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `description` TEXT COMMENT '操作描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 6. 私密空间表 (Private Space)
CREATE TABLE IF NOT EXISTS `private_space` (
    `userId` BIGINT PRIMARY KEY COMMENT '用户ID (主键，关联用户表)',
    `password` VARCHAR(255) NOT NULL COMMENT '私密空间密码',
    `isEncrypted` TINYINT DEFAULT 1 COMMENT '是否启用 (0=否, 1=是)',
    FOREIGN KEY (`userId`) REFERENCES `user`(`userId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私密空间表';
