package org.example.netdisk.Service.Support;

public class FileConflictException extends RuntimeException { // 文件名冲突异常：前端捕获后弹出替换确认弹窗
    private final String conflictName; // 冲突的文件名

    public FileConflictException(String conflictName) { // 构造冲突异常
        super("文件 " + conflictName + " 已存在，是否替换？"); // 设置异常提示信息
        this.conflictName = conflictName; // 保存冲突文件名供前端使用
    }

    public String getConflictName() { // 获取冲突文件名
        return conflictName;
    }
}
