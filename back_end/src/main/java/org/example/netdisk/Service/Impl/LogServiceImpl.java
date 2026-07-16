package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Service.Inter.LogService;
import org.example.netdisk.Mapper.LogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private LogMapper logmapper; // 日志表数据库操作接口

    @Override
    public void logMethodExecution(String userId, String operation) { // 记录用户操作日志：解析userId→拼接操作描述→插入日志表
        try {
            Long operatorId = Long.parseLong(userId); // 将字符串userId转为Long型操作者ID
            logmapper.insertLog(operatorId, "访问接口: " + operation); // 插入日志记录，格式为"访问接口: 具体操作名"
        } catch (NumberFormatException e) { // userId不是合法数字格式
            log.warn("无法记录日志，userId 非法: {}", userId); // 记录警告日志，不中断业务
        }
    }
}
