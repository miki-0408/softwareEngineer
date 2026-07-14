package org.example.netdisk.Controller;

import org.example.netdisk.Entity.NetdiskFile;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.FileService;
import org.example.netdisk.Utils.TokenProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock private FileService fileService;
    @Mock private FileMapper fileMapper;
    @InjectMocks private FileController controller;
    private MockedStatic<TokenProcess> tokenMock;

    @BeforeEach
    void setUp() {
        tokenMock = mockStatic(TokenProcess.class);
        tokenMock.when(() -> TokenProcess.getAttributeFromToken(anyString(), eq("userId"))).thenReturn("1");
    }

    @AfterEach
    void tearDown() { tokenMock.close(); }

    // ==================== uploadFile ====================
    @Test
    void uploadFile_shouldReturnSuccess() throws Exception {
        R_File mockFile = new R_File(); mockFile.setFileId("100");
        when(fileService.uploadFiles(anyLong(), anyLong(), any(), any(), anyBoolean(), any(), anyString(), anyString(), any()))
                .thenReturn(mockFile);

        List<MultipartFile> files = List.of(new MockMultipartFile("files", "test.txt", "text/plain", "data".getBytes()));
        Result<R_File> result = controller.uploadFile("Bearer x", 1L, files, null, false, null, "none", "lz77", null);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals("100", result.getData().getFileId());
        verify(fileService).uploadFiles(anyLong(), eq(1L), eq(files), any(), eq(false), eq(null), eq("none"), eq("lz77"), eq(null));
    }

    // ==================== downloadFile ====================
    @Test
    void downloadFile_shouldReturnFile() throws Exception {
        NetdiskFile nf = new NetdiskFile(); nf.setFileName("test.txt");
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(nf);
        when(fileService.downloadFile(1L, 1L, null)).thenReturn("hello".getBytes());

        ResponseEntity<byte[]> resp = controller.downloadFile("Bearer x", 1L, null);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        verify(fileMapper).selectFileById(1L, 1L);
    }

    @Test
    void downloadFile_shouldReturn404_whenFileNotFound() throws Exception {
        when(fileMapper.selectFileById(99L, 1L)).thenReturn(null);
        ResponseEntity<byte[]> resp = controller.downloadFile("Bearer x", 99L, null);
        assertEquals(404, resp.getStatusCode().value());
    }

    // ==================== listFiles ====================
    @Test
    void listFiles_shouldReturnList() throws Exception {
        R_File f = new R_File(); f.setFileId("1");
        when(fileService.listFiles(1L, 10L)).thenReturn(List.of(f));

        Result<List<R_File>> result = controller.listFiles("Bearrer x", 10L);

        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
    }

    // ==================== renameFile ====================
    @Test
    void renameFile_shouldReturnSuccess() throws Exception {
        when(fileService.renameFile(1L, 1L, "new.txt", false)).thenReturn(true);
        Result<String> r = controller.renameFile("Bearer x", 1L, "new.txt", false);
        assertEquals(0, r.getCode());
    }

    @Test
    void renameFile_shouldReturnError() throws Exception {
        when(fileService.renameFile(1L, 1L, "new.txt", false)).thenReturn(false);
        Result<String> r = controller.renameFile("Bearer x", 1L, "new.txt", false);
        assertNotEquals(0, r.getCode());
    }

    // ==================== moveFile ====================
    @Test
    void moveFile_shouldReturnSuccess() throws Exception {
        when(fileService.moveFile(1L, 1L, 2L, true)).thenReturn(true);
        Result<String> r = controller.moveFile("Bearer x", 1L, 2L, true);
        assertEquals(0, r.getCode());
    }

    // ==================== deleteFile ====================
    @Test
    void deleteFile_shouldReturnSuccess() throws Exception {
        when(fileService.moveToRecycleBin(1L, 1L)).thenReturn(true);
        Result<String> r = controller.deleteFile("Bearer x", 1L);
        assertEquals(0, r.getCode());
    }

    // ==================== encryptFile ====================
    @Test
    void encryptFile_shouldReturnSuccess() throws Exception {
        when(fileService.encryptFile(1L, 1L, "pwd", 2L, false)).thenReturn(true);
        Result<String> r = controller.encryptFile("Bearer x", 1L, "pwd", 2L, false);
        assertEquals(0, r.getCode());
    }

    @Test
    void encryptFile_shouldReturnError() throws Exception {
        when(fileService.encryptFile(1L, 1L, "pwd", 2L, false)).thenReturn(false);
        Result<String> r = controller.encryptFile("Bearer x", 1L, "pwd", 2L, false);
        assertNotEquals(0, r.getCode());
    }

    // ==================== decryptFile ====================
    @Test
    void decryptFile_shouldReturnSuccess() throws Exception {
        when(fileService.decryptFile(1L, 1L, "pwd", 2L, false)).thenReturn(true);
        Result<String> r = controller.decryptFile("Bearer x", 1L, "pwd", 2L, false);
        assertEquals(0, r.getCode());
    }

    @Test
    void decryptFile_shouldReturnError() throws Exception {
        when(fileService.decryptFile(1L, 1L, "pwd", 2L, false)).thenReturn(false);
        Result<String> r = controller.decryptFile("Bearer x", 1L, "pwd", 2L, false);
        assertNotEquals(0, r.getCode());
    }
}
