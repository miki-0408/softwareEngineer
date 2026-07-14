package org.example.netdisk.Controller;

import org.example.netdisk.ResponseDTO.R_Directory;
import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Inter.DirectoryService;
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
class DirectoryControllerTest {

    @Mock private DirectoryService directoryService;
    @InjectMocks private DirectoryController controller;
    private MockedStatic<TokenProcess> tokenMock;

    @BeforeEach
    void setUp() {
        tokenMock = mockStatic(TokenProcess.class);
        tokenMock.when(() -> TokenProcess.getAttributeFromToken(anyString(), eq("userId"))).thenReturn("1");
    }

    @AfterEach
    void tearDown() { tokenMock.close(); }

    @Test
    void createDirectory_shouldReturnSuccess() throws Exception {
        R_Directory dir = new R_Directory(); dir.setDirId("10");
        when(directoryService.createDirectory(eq(1L), eq("testDir"), isNull())).thenReturn(dir);

        Result<R_Directory> r = controller.createDirectory("Bearer x", "testDir", null);
        assertEquals(0, r.getCode());
        assertEquals("10", r.getData().getDirId());
    }

    @Test
    void createDirectory_withParentDirId() throws Exception {
        R_Directory dir = new R_Directory(); dir.setDirId("11");
        when(directoryService.createDirectory(eq(1L), eq("subDir"), eq(5L))).thenReturn(dir);

        Result<R_Directory> r = controller.createDirectory("Bearer x", "subDir", 5L);
        assertEquals(0, r.getCode());
    }

    @Test
    void listDirectories_shouldReturnList() throws Exception {
        R_Directory d1 = new R_Directory(); d1.setDirId("1");
        when(directoryService.listDirectories(eq(1L), isNull())).thenReturn(List.of(d1));

        Result<List<R_Directory>> r = controller.listDirectories("Bearer x", null);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    void listDirectories_withParentId() throws Exception {
        when(directoryService.listDirectories(eq(1L), eq(5L))).thenReturn(List.of());
        Result<List<R_Directory>> r = controller.listDirectories("Bearer x", 5L);
        assertEquals(0, r.getCode());
    }

    @Test
    void renameDirectory_shouldReturnSuccess() throws Exception {
        when(directoryService.renameDirectory(1L, 10L, "newName")).thenReturn(true);
        Result<String> r = controller.renameDirectory("Bearer x", 10L, "newName");
        assertEquals(0, r.getCode());
    }

    @Test
    void renameDirectory_shouldReturnError() throws Exception {
        when(directoryService.renameDirectory(1L, 10L, "newName")).thenReturn(false);
        Result<String> r = controller.renameDirectory("Bearer x", 10L, "newName");
        assertNotEquals(0, r.getCode());
    }

    @Test
    void deleteDirectory_shouldReturnSuccess() throws Exception {
        when(directoryService.deleteDirectory(1L, 10L)).thenReturn(true);
        Result<String> r = controller.deleteDirectory("Bearer x", 10L);
        assertEquals(0, r.getCode());
    }

    @Test
    void deleteDirectory_shouldReturnError() throws Exception {
        when(directoryService.deleteDirectory(1L, 10L)).thenReturn(false);
        Result<String> r = controller.deleteDirectory("Bearer x", 10L);
        assertNotEquals(0, r.getCode());
    }
}
