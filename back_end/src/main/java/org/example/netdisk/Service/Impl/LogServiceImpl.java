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
    private LogMapper logmapper;

    @Override
    public void logMethodExecution(String userId, String operation) {
        try {
            Long operatorId = Long.parseLong(userId);
            logmapper.insertLog(operatorId, "访问接口: " + operation);
        } catch (NumberFormatException e) {
            log.warn("无法记录日志，userId 非法: {}", userId);
        }
    }
}
