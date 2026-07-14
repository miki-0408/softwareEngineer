package org.example.netdisk.Service;

import org.example.netdisk.Entity.*;
import org.example.netdisk.Mapper.*;
import org.example.netdisk.ResponseDTO.*;
import org.example.netdisk.Service.Impl.PrivateSpaceServiceImpl;
import org.example.netdisk.Service.Support.TransformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.example.netdisk.Service.Support.Enum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivateSpaceServiceImplTest {

    @Mock private PrivateSpaceMapper privateSpaceMapper;
    @Mock private UserMapper userMapper;
    @Mock private DirectoryMapper directoryMapper;
    @Mock private FileMapper fileMapper;
    @Mock private TransformService transformService;
    @InjectMocks private PrivateSpaceServiceImpl privateSpaceService;

    @Test
    void enablePrivateSpace_shouldSucceed_whenNew() {
        when(privateSpaceMapper.selectByUserId(1L)).thenReturn(null);
        doNothing().when(privateSpaceMapper).insertPrivateSpace(any());
        when(directoryMapper.selectRootDirectories(1L)).thenReturn(List.of());
        doNothing().when(directoryMapper).insertDirectory(any());

        assertTrue(privateSpaceService.enablePrivateSpace(1L, "pwd123"));
        verify(privateSpaceMapper).insertPrivateSpace(any());
    }

    @Test
    void enablePrivateSpace_shouldUpdate_whenExists() {
        PrivateSpace existing = new PrivateSpace();
        existing.setUserId(1L); existing.setIsEncrypted(privateSpaceDisabled);
        when(privateSpaceMapper.selectByUserId(1L)).thenReturn(existing);
        when(directoryMapper.selectRootDirectories(1L)).thenReturn(List.of());

        assertTrue(privateSpaceService.enablePrivateSpace(1L, "pwd123"));
    }

    @Test
    void enablePrivateSpace_shouldThrow_whenBlankPassword() {
        assertThrows(RuntimeException.class,
            () -> privateSpaceService.enablePrivateSpace(1L, ""));
        assertThrows(RuntimeException.class,
            () -> privateSpaceService.enablePrivateSpace(1L, "   "));
    }

    @Test
    void enablePrivateSpace_shouldThrow_whenAlreadyEnabled() {
        PrivateSpace existing = new PrivateSpace();
        existing.setIsEncrypted(privateSpaceEnabled);
        when(privateSpaceMapper.selectByUserId(1L)).thenReturn(existing);
        assertThrows(RuntimeException.class,
            () -> privateSpaceService.enablePrivateSpace(1L, "pwd"));
    }

    @Test
    void disablePrivateSpace_shouldReturnFalse_whenNotEnabled() {
        when(privateSpaceMapper.selectByUserId(1L)).thenReturn(null);
        assertFalse(privateSpaceService.disablePrivateSpace(1L, "pwd"));
    }

    @Test
    void validatePrivatePassword_shouldThrow_whenNotEnabled() {
        when(privateSpaceMapper.selectByUserId(1L)).thenReturn(null);
        assertThrows(RuntimeException.class,
            () -> privateSpaceService.validatePrivatePassword(1L, "pwd"));
    }

    @Test
    void getPrivateSpaceStatus_shouldReturnDisabled_whenNull() {
        when(privateSpaceMapper.selectByUserId(1L)).thenReturn(null);
        R_PrivateSpace rps = new R_PrivateSpace(); rps.setEnabled(false);
        when(transformService.transformPrivateSpaceToRPrivateSpace(null)).thenReturn(rps);

        R_PrivateSpace result = privateSpaceService.getPrivateSpaceStatus(1L);
        assertFalse(result.getEnabled());
    }
}
