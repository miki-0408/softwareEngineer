package org.example.netdisk.Service.Inter;

public interface LogService { // 日志服务接口：定义用户操作日志记录

    void logMethodExecution(String userId, String operation); // 记录用户操作（用户ID + 操作描述）
}
