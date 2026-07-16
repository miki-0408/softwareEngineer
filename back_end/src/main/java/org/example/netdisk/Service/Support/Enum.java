package org.example.netdisk.Service.Support;

public class Enum { // 全局常量定义类：统一管理系统中使用的字符串和数值常量
    public static final String unknown = "未知"; // 未知角色/状态的默认值

    public static final String normalUser = "user"; // 普通用户角色标识
    public static final String systemAdmin = "admin"; // 系统管理员角色标识

    public static final int fileStatusNormal = 0; // 文件正常状态
    public static final int fileStatusRecycle = 1; // 文件回收站状态

    public static final int notEncrypted = 0; // 文件未加密
    public static final int encrypted = 1; // 文件已加密

    public static final int privateSpaceDisabled = 0; // 私密空间未启用
    public static final int privateSpaceEnabled = 1; // 私密空间已启用

    public static final long defaultTotalSpace = 10737418240L; // 默认总空间10GB（字节）
    public static final String defaultPWD = "password123"; // 默认密码
    public static final String rootDirName = "我的文件"; // 用户根目录默认名称
    public static final String privateSpaceRootDirName = "私密空间"; // 私密空间根目录名称
}
