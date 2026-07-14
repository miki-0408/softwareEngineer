package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.*;
import org.example.netdisk.Service.Inter.UserService;
import org.example.netdisk.Utils.TokenProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @InjectMocks private UserController controller;
    private MockedStatic<TokenProcess> tokenMock;

    @BeforeEach
    void setUp() {
        tokenMock = mockStatic(TokenProcess.class);
        tokenMock.when(() -> TokenProcess.getAttributeFromToken(anyString(), eq("userId"))).thenReturn("1");
    }

    @AfterEach
    void tearDown() { tokenMock.close(); }

    @Test
    void register_shouldReturnSuccess() {
        when(userService.register(eq("user1"), eq("pwd"), eq(null), isNull())).thenReturn(true);
        Result<String> r = controller.register("user1", "pwd", null, null);
        assertEquals(0, r.getCode());
    }

    @Test
    void register_shouldReturnError_whenUsernameExists() {
        when(userService.register(eq("user1"), eq("pwd"), eq(null), isNull())).thenReturn(false);
        Result<String> r = controller.register("user1", "pwd", null, null);
        assertNotEquals(0, r.getCode());
    }

    @Test
    void login_shouldReturnSuccess() {
        R_LoginDTO dto = new R_LoginDTO();
        R_User user = new R_User(); user.setUserId("1"); dto.setUser(user);
        when(userService.login("user1", "pwd")).thenReturn(Map.of("rLoginDTO", dto, "message", "success"));

        Result<R_LoginDTO> r = controller.login("user1", "pwd");
        assertEquals(0, r.getCode());
    }

    @Test
    void login_shouldReturnError_whenInvalid() {
        when(userService.login("user1", "wrong")).thenReturn(Map.of("rLoginDTO", "null", "message", "密码错误"));

        Result<R_LoginDTO> r = controller.login("user1", "wrong");
        assertNotEquals(0, r.getCode());
    }

    @Test
    void getUserInfo_shouldReturnSuccess() throws Exception {
        R_UserInfoDTO dto = new R_UserInfoDTO();
        R_User user = new R_User(); user.setUserId("2"); dto.setUser(user);
        when(userService.getUserInfo(1L, 2L)).thenReturn(dto);

        Result<R_UserInfoDTO> r = controller.getUserInfo("Bearer x", 2L);
        assertEquals(0, r.getCode());
    }

    @Test
    void getUserInfo_shouldReturnError_whenUserNotFound() throws Exception {
        when(userService.getUserInfo(1L, 99L)).thenReturn(null);
        Result<R_UserInfoDTO> r = controller.getUserInfo("Bearer x", 99L);
        assertNotEquals(0, r.getCode());
    }

    @Test
    void changePassword_shouldReturnSuccess() throws Exception {
        when(userService.changePassword(1L, "old", "new")).thenReturn(true);
        Result<String> r = controller.changePassword("Bearer x", "old", "new");
        assertEquals(0, r.getCode());
    }

    @Test
    void changePassword_shouldReturnError() throws Exception {
        when(userService.changePassword(1L, "old", "wrong")).thenReturn(false);
        Result<String> r = controller.changePassword("Bearer x", "old", "wrong");
        assertNotEquals(0, r.getCode());
    }

    @Test
    void updateUserInfo_shouldReturnSuccess() throws Exception {
        when(userService.updateUserInfo(eq(1L), eq("newname"), eq(null), isNull())).thenReturn(true);
        Result<String> r = controller.updateUserInfo("Bearer x", "newname", null, null);
        assertEquals(0, r.getCode());
    }

    @Test
    void updateUserInfo_shouldReturnError() throws Exception {
        when(userService.updateUserInfo(eq(1L), eq("newname"), eq(null), isNull())).thenReturn(false);
        Result<String> r = controller.updateUserInfo("Bearer x", "newname", null, null);
        assertNotEquals(0, r.getCode());
    }

    @Test
    void register_withAvatar() {
        MockMultipartFile avatar = new MockMultipartFile("avatar", "pic.jpg", "image/jpeg", "img".getBytes());
        when(userService.register(eq("u"), eq("p"), eq("男"), eq(avatar))).thenReturn(true);
        Result<String> r = controller.register("u", "p", "男", avatar);
        assertEquals(0, r.getCode());
    }
}
