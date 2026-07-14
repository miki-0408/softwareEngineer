package org.example.netdisk.Service;

import org.example.netdisk.Entity.*;
import org.example.netdisk.Mapper.*;
import org.example.netdisk.ResponseDTO.*;
import org.example.netdisk.Service.Impl.UserServiceImpl;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Service.Support.TransformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserMapper usermapper;
    @Mock private StorageSpaceMapper storageSpaceMapper;
    @Mock private DirectoryMapper directoryMapper;
    @Mock private PrivateSpaceMapper privateSpaceMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private TransformService transformService;
    @InjectMocks private UserServiceImpl userService;

    @Test
    void register_shouldSucceed() {
        when(usermapper.selectUserByName("newUser")).thenReturn(null);
        doAnswer((Answer<Void>) inv -> { User u = inv.getArgument(0); u.setUserId(1L); return null; })
            .when(usermapper).insertUser(any());

        assertTrue(userService.register("newUser", "pwd123", "male", null));
        verify(storageSpaceMapper).insertStorageSpace(any());
        verify(directoryMapper).insertDirectory(any());
    }

    @Test
    void register_shouldReturnFalse_whenUserExists() {
        when(usermapper.selectUserByName("exist")).thenReturn(new User());
        assertFalse(userService.register("exist", "pwd", null, null));
    }

    @Test
    void register_shouldThrow_whenAvatarNotImage() {
        when(usermapper.selectUserByName("user1")).thenReturn(null);
        MockMultipartFile badAvatar = new MockMultipartFile("avatar", "bad.pdf",
            "application/pdf", "data".getBytes());
        assertThrows(RuntimeException.class,
            () -> userService.register("user1", "pwd", null, badAvatar));
    }

    @Test
    void login_shouldReturnUserNotFound() {
        when(usermapper.selectUserByName("noOne")).thenReturn(null);
        Map<String, Object> result = userService.login("noOne", "pwd");
        assertEquals("用户不存在", result.get("message"));
    }

    @Test
    void getUserInfo_shouldReturnDto() {
        User u = new User(); u.setUserId(2L); u.setName("u2");
        when(usermapper.selectUserById(2L)).thenReturn(u);
        R_User ru = new R_User(); ru.setUserId("2");
        when(transformService.transformUserToRUser(u)).thenReturn(ru);
        StorageSpace sp = new StorageSpace(); sp.setTotalSpace(1000L);
        when(storageSpaceMapper.selectByUserId(2L)).thenReturn(sp);
        R_StorageSpace rsp = new R_StorageSpace();
        when(transformService.transformStorageSpaceToRStorageSpace(sp)).thenReturn(rsp);
        PrivateSpace ps = new PrivateSpace();
        when(privateSpaceMapper.selectByUserId(2L)).thenReturn(ps);
        R_PrivateSpace rps = new R_PrivateSpace();
        when(transformService.transformPrivateSpaceToRPrivateSpace(ps)).thenReturn(rps);

        R_UserInfoDTO dto = userService.getUserInfo(1L, 2L);
        assertNotNull(dto);
        assertEquals("2", dto.getUser().getUserId());
    }

    @Test
    void getUserInfo_shouldReturnNull_whenUserNotFound() {
        when(usermapper.selectUserById(99L)).thenReturn(null);
        assertNull(userService.getUserInfo(1L, 99L));
    }

    @Test
    void changePassword_shouldReturnFalse_whenUserNotFound() {
        when(usermapper.selectUserById(99L)).thenReturn(null);
        assertFalse(userService.changePassword(99L, "old", "new"));
    }

    @Test
    void updateUserInfo_shouldReturnFalse_whenUserNotFound() {
        when(usermapper.selectUserById(99L)).thenReturn(null);
        assertFalse(userService.updateUserInfo(99L, "newname", null, null));
    }

    @Test
    void updateUserInfo_shouldUpdateNameAndGender() {
        User u = new User(); u.setUserId(1L); u.setName("old");
        when(usermapper.selectUserById(1L)).thenReturn(u);
        when(usermapper.selectUserByName("newName")).thenReturn(null);
        doAnswer((Answer<Void>) inv -> { return null; }).when(usermapper).updateUser(any());

        assertTrue(userService.updateUserInfo(1L, "newName", "female", null));
        assertEquals("newName", u.getName());
        assertEquals("female", u.getSex());
    }
}
