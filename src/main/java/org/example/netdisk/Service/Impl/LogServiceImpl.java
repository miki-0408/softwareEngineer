package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.PCOI.Mapper.LogMapper;
import org.example.PCOI.Service.Inter.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogServiceImpl implements LogService {
    @Autowired
    private LogMapper logMapper;

    @Override
    public void logMethodExecution(String userId, String operation) {
        logMapper.insertLog(userId, operation);
    }
}

