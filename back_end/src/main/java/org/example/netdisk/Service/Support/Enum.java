package org.example.netdisk.Service.Support;

public class Enum {
    public static final String male = "男";
    public static final String female = "女";
    public static final String unknown = "未知";

    public static final String normalUser = "user";
    public static final String systemAdmin = "admin";

    public static final int fileStatusNormal = 0;
    public static final int fileStatusRecycle = 1;

    public static final int notEncrypted = 0;
    public static final int encrypted = 1;

    public static final int privateSpaceDisabled = 0;
    public static final int privateSpaceEnabled = 1;

    public static final int login = 0;
    public static final int updatePWD = 1;
    public static final int privateSpaceVerify = 2;

    public static final long defaultTotalSpace = 10737418240L;
    public static final String defaultPWD = "password123";
    public static final String rootDirName = "我的文件";
    public static final String privateSpaceRootDirName = "私密空间";
}
