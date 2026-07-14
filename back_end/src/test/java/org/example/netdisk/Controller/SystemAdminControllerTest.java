package org.example.netdisk.Controller;

import org.example.netdisk.Entity.Log;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.SystemAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemAdminControllerTest {

    @Mock private SystemAdminService systemAdminService;
    @InjectMocks private SystemAdminController controller;

    @Test
    void updateUserInfo_shouldReturnSuccess() {
        when(systemAdminService.updateUserInfo(eq(1L), eq("newname"), isNull(), isNull())).thenReturn(true);
        Result<String> r = controller.updateUserInfo(1L, "newname", null, null);
        assertEquals(0, r.getCode());
    }

    @Test
    void updateUserInfo_shouldReturnError() {
        when(systemAdminService.updateUserInfo(eq(1L), eq("newname"), isNull(), isNull())).thenReturn(false);
        Result<String> r = controller.updateUserInfo(1L, "newname", null, null);
        assertNotEquals(0, r.getCode());
    }

    @Test
    void updateUserInfo_shouldReturnError_whenRuntimeException() {
        when(systemAdminService.updateUserInfo(anyLong(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("用户不存在"));
        Result<String> r = controller.updateUserInfo(99L, "newname", null, null);
        assertNotEquals(0, r.getCode());
    }

    @Test
    void resetPassword_shouldReturnSuccess() {
        when(systemAdminService.resetPassword(1L)).thenReturn(true);
        Result<String> r = controller.resetPassword(1L);
        assertEquals(0, r.getCode());
    }

    @Test
    void resetPassword_shouldReturnError() {
        when(systemAdminService.resetPassword(99L)).thenReturn(false);
        Result<String> r = controller.resetPassword(99L);
        assertNotEquals(0, r.getCode());
    }

    @Test
    void getLogs_shouldReturnList() {
        Log log = new Log(); log.setLogId(1L); log.setOperatorId(1L);
        log.setDescription("test"); log.setTime(LocalDateTime.now());
        when(systemAdminService.getLogs()).thenReturn(List.of(log));

        Result<List<Log>> r = controller.getLogs();
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    void getLogs_shouldReturnEmpty() {
        when(systemAdminService.getLogs()).thenReturn(List.of());
        Result<List<Log>> r = controller.getLogs();
        assertEquals(0, r.getCode());
        assertTrue(r.getData().isEmpty());
    }
}
