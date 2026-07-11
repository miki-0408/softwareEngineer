package org.example.netdisk.Service.Support;

/** 文件名冲突异常——前端捕获后弹出替换确认弹窗 */
public class FileConflictException extends RuntimeException {
    private final String conflictName;

    public FileConflictException(String conflictName) {
        super("文件 " + conflictName + " 已存在，是否替换？");
        this.conflictName = conflictName;
    }

    public String getConflictName() {
        return conflictName;
    }
}
