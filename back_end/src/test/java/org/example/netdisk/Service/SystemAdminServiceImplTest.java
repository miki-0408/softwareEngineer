package org.example.netdisk.Service;

import org.example.netdisk.Entity.Log;
import org.example.netdisk.Entity.User;
import org.example.netdisk.Mapper.LogMapper;
import org.example.netdisk.Mapper.UserMapper;
import org.example.netdisk.Service.Impl.SystemAdminServiceImpl;
import org.example.netdisk.Service.Impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemAdminServiceImplTest {

    @Mock private UserMapper usermapper;
    @Mock private LogMapper logmapper;
    @Mock private UserServiceImpl userService;
    @InjectMocks private SystemAdminServiceImpl systemAdminService;

    @Test
    void resetPassword_shouldSucceed() {
        User u = new User(); u.setUserId(1L);
        when(usermapper.selectUserById(1L)).thenReturn(u);

        assertTrue(systemAdminService.resetPassword(1L));
        verify(usermapper).updateUser(u);
    }

    @Test
    void resetPassword_shouldReturnFalse_whenUserNotFound() {
        when(usermapper.selectUserById(99L)).thenReturn(null);
        assertFalse(systemAdminService.resetPassword(99L));
    }

    @Test
    void getLogs_shouldReturnList() {
        Log log = new Log(); log.setLogId(1L); log.setDescription("test");
        log.setOperatorId(1L); log.setTime(LocalDateTime.now());
        when(logmapper.selectAllLogs()).thenReturn(List.of(log));

        List<Log> result = systemAdminService.getLogs();
        assertEquals(1, result.size());
    }

    @Test
    void getLogs_shouldReturnEmpty() {
        when(logmapper.selectAllLogs()).thenReturn(List.of());
        assertTrue(systemAdminService.getLogs().isEmpty());
    }

    @Test
    void updateUserInfo_shouldDelegate() {
        when(userService.updateUserInfo(eq(1L), eq("n"), isNull(), isNull())).thenReturn(true);
        assertTrue(systemAdminService.updateUserInfo(1L, "n", null, null));
        verify(userService).updateUserInfo(1L, "n", null, null);
    }
}
