package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.RecycleBinService;
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
class RecycleBinControllerTest {

    @Mock private RecycleBinService recycleBinService;
    @InjectMocks private RecycleBinController controller;
    private MockedStatic<TokenProcess> tokenMock;

    @BeforeEach
    void setUp() {
        tokenMock = mockStatic(TokenProcess.class);
        tokenMock.when(() -> TokenProcess.getAttributeFromToken(anyString(), eq("userId"))).thenReturn("1");
    }

    @AfterEach
    void tearDown() { tokenMock.close(); }

    @Test
    void listRecycleFiles_shouldReturnList() throws Exception {
        R_File f = new R_File(); f.setFileId("1");
        when(recycleBinService.listRecycleFiles(1L)).thenReturn(List.of(f));

        Result<List<R_File>> r = controller.listRecycleFiles("Bearer x");
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    void restoreFile_shouldReturnSuccess() throws Exception {
        when(recycleBinService.restoreFile(1L, 10L)).thenReturn(true);
        Result<String> r = controller.restoreFile("Bearer x", 10L);
        assertEquals(0, r.getCode());
    }

    @Test
    void restoreFile_shouldReturnError() throws Exception {
        when(recycleBinService.restoreFile(1L, 10L)).thenReturn(false);
        Result<String> r = controller.restoreFile("Bearer x", 10L);
        assertNotEquals(0, r.getCode());
    }

    @Test
    void deletePermanently_shouldReturnSuccess() throws Exception {
        when(recycleBinService.deletePermanently(1L, 10L)).thenReturn(true);
        Result<String> r = controller.deletePermanently("Bearer x", 10L);
        assertEquals(0, r.getCode());
    }

    @Test
    void deletePermanently_shouldReturnError() throws Exception {
        when(recycleBinService.deletePermanently(1L, 10L)).thenReturn(false);
        Result<String> r = controller.deletePermanently("Bearer x", 10L);
        assertNotEquals(0, r.getCode());
    }
}
