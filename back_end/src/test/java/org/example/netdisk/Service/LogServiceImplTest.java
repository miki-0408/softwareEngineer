package org.example.netdisk.Service;

import org.example.netdisk.Mapper.LogMapper;
import org.example.netdisk.Service.Impl.LogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    @Mock private LogMapper logmapper;
    @InjectMocks private LogServiceImpl logService;

    @Test
    void logMethodExecution_shouldInsertLog() {
        logService.logMethodExecution("1", "test operation");
        verify(logmapper).insertLog(eq(1L), contains("test operation"));
    }

    @Test
    void logMethodExecution_shouldNotThrow_whenBadUserId() {
        // Should not throw on invalid userId — just warn
        assertDoesNotThrow(() -> logService.logMethodExecution("abc", "test"));
        verify(logmapper, never()).insertLog(anyLong(), anyString());
        // second call with empty userId
        assertDoesNotThrow(() -> logService.logMethodExecution("", "test"));
    }

    @Test
    void logMethodExecution_shouldInsert_correctOperationName() {
        logService.logMethodExecution("5", "file upload");
        verify(logmapper).insertLog(eq(5L), eq("访问接口: file upload"));
    }
}
