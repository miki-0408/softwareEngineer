package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.*;
import org.example.netdisk.Service.Impl.FileServiceImpl;
import org.example.netdisk.Service.Inter.PrivateSpaceService;
import org.example.netdisk.Utils.TokenProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivateSpaceControllerTest {

    @Mock private PrivateSpaceService privateSpaceService;
    @Mock private FileServiceImpl fileService;
    @InjectMocks private PrivateSpaceController controller;
    private MockedStatic<TokenProcess> tokenMock;

    @BeforeEach
    void setUp() {
        tokenMock = mockStatic(TokenProcess.class);
        tokenMock.when(() -> TokenProcess.getAttributeFromToken(anyString(), eq("userId"))).thenReturn("1");
    }

    @AfterEach
    void tearDown() { tokenMock.close(); }

    @Test
    void enablePrivateSpace_shouldReturnSuccess() throws Exception {
        when(privateSpaceService.enablePrivateSpace(1L, "pwd")).thenReturn(true);
        Result<String> r = controller.enablePrivateSpace("Bearer x", "pwd");
        assertEquals(0, r.getCode());
    }

    @Test
    void enablePrivateSpace_shouldReturnError() throws Exception {
        when(privateSpaceService.enablePrivateSpace(1L, "pwd")).thenReturn(false);
        Result<String> r = controller.enablePrivateSpace("Bearer x", "pwd");
        assertNotEquals(0, r.getCode());
    }

    @Test
    void disablePrivateSpace_shouldReturnSuccess() throws Exception {
        when(privateSpaceService.disablePrivateSpace(eq(1L), eq("pwd"))).thenReturn(true);
        Result<String> r = controller.disablePrivateSpace("Bearer x", "pwd");
        assertEquals(0, r.getCode());
    }

    @Test
    void verifyPrivateSpace_shouldReturnSuccess() throws Exception {
        R_VerifyPrivateSpaceDTO dto = new R_VerifyPrivateSpaceDTO(); dto.setVerified(true);
        when(privateSpaceService.verifyPrivateSpace(1L, "pwd")).thenReturn(dto);
        Result<R_VerifyPrivateSpaceDTO> r = controller.verifyPrivateSpace("Bearer x", "pwd");
        assertEquals(0, r.getCode());
        assertTrue(r.getData().getVerified());
    }

    @Test
    void verifyPrivateSpace_shouldReturnError_whenNull() throws Exception {
        when(privateSpaceService.verifyPrivateSpace(1L, "pwd")).thenReturn(null);
        Result<R_VerifyPrivateSpaceDTO> r = controller.verifyPrivateSpace("Bearer x", "pwd");
        assertNotEquals(0, r.getCode());
    }

    @Test
    void verifyPrivateSpace_shouldReturnError_whenInvalid() throws Exception {
        R_VerifyPrivateSpaceDTO dto = new R_VerifyPrivateSpaceDTO(); dto.setVerified(false);
        when(privateSpaceService.verifyPrivateSpace(1L, "pwd")).thenReturn(dto);
        Result<R_VerifyPrivateSpaceDTO> r = controller.verifyPrivateSpace("Bearer x", "pwd");
        assertNotEquals(0, r.getCode());
    }

    @Test
    void getPrivateSpaceStatus_shouldReturnStatus() throws Exception {
        R_PrivateSpace ps = new R_PrivateSpace(); ps.setEnabled(true);
        when(privateSpaceService.getPrivateSpaceStatus(1L)).thenReturn(ps);
        Result<R_PrivateSpace> r = controller.getPrivateSpaceStatus("Bearer x");
        assertEquals(0, r.getCode());
    }

    @Test
    void listPrivateFiles_shouldReturnFiles() throws Exception {
        R_File f = new R_File(); f.setFileId("10");
        when(fileService.listPrivateFiles(1L, 5L)).thenReturn(List.of(f));
        Result<List<R_File>> r = controller.listPrivateFiles("Bearer x", 5L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    void listPrivateDirectories_shouldReturnDirs() throws Exception {
        R_Directory d = new R_Directory(); d.setDirId("1");
        when(fileService.listPrivateDirectories(1L, null)).thenReturn(List.of(d));
        Result<List<R_Directory>> r = controller.listPrivateDirectories("Bearer x", null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }
}
