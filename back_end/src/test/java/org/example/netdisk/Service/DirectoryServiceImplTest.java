package org.example.netdisk.Service;

import org.example.netdisk.Entity.Directory;
import org.example.netdisk.Mapper.DirectoryMapper;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.ResponseDTO.R_Directory;
import org.example.netdisk.Service.Impl.DirectoryServiceImpl;
import org.example.netdisk.Service.Support.TransformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceImplTest {

    @Mock private DirectoryMapper directoryMapper;
    @Mock private FileMapper fileMapper;
    @Mock private TransformService transformService;
    @InjectMocks private DirectoryServiceImpl directoryService;

    @Test
    void createDirectory_shouldSucceed() {
        when(directoryMapper.countDirsByName(anyLong(), any(), anyString())).thenReturn(0);
        R_Directory rd = new R_Directory(); rd.setDirId("10"); rd.setDirName("testDir");
        when(transformService.transformDirectoryToRDirectory(any())).thenReturn(rd);

        R_Directory result = directoryService.createDirectory(1L, "testDir", null);
        assertNotNull(result);
        assertEquals("10", result.getDirId());
        verify(directoryMapper).insertDirectory(any());
    }

    @Test
    void createDirectory_shouldThrow_whenParentNotFound() {
        when(directoryMapper.selectDirectoryById(5L, 1L)).thenReturn(null);
        assertThrows(RuntimeException.class,
            () -> directoryService.createDirectory(1L, "sub", 5L));
    }

    @Test
    void listDirectories_shouldReturnRoots_whenParentNull() {
        Directory d = new Directory(); d.setDirId(1L); d.setDirName("root");
        when(directoryMapper.selectRootDirectories(1L)).thenReturn(List.of(d));
        R_Directory rd = new R_Directory(); rd.setDirId("1");
        when(transformService.transformDirectoryToRDirectory(d)).thenReturn(rd);

        List<R_Directory> result = directoryService.listDirectories(1L, null);
        assertEquals(1, result.size());
        verify(directoryMapper).selectRootDirectories(1L);
    }

    @Test
    void listDirectories_shouldReturnChildren_whenParentNotNull() {
        Directory d = new Directory(); d.setDirId(2L);
        when(directoryMapper.selectDirectoriesByParentId(1L, 5L)).thenReturn(List.of(d));
        R_Directory rd = new R_Directory(); rd.setDirId("2");
        when(transformService.transformDirectoryToRDirectory(d)).thenReturn(rd);

        List<R_Directory> result = directoryService.listDirectories(1L, 5L);
        assertEquals(1, result.size());
        verify(directoryMapper).selectDirectoriesByParentId(1L, 5L);
    }

    @Test
    void renameDirectory_shouldSucceed() {
        Directory d = new Directory(); d.setDirId(10L); d.setParentDirId(null);
        when(directoryMapper.selectDirectoryById(10L, 1L)).thenReturn(d);
        when(directoryMapper.countDirsByName(1L, null, "newName")).thenReturn(0);
        when(directoryMapper.updateDirectoryName(any())).thenReturn(1);

        assertTrue(directoryService.renameDirectory(1L, 10L, "newName"));
    }

    @Test
    void renameDirectory_shouldReturnFalse_whenNotFound() {
        when(directoryMapper.selectDirectoryById(99L, 1L)).thenReturn(null);
        assertFalse(directoryService.renameDirectory(1L, 99L, "x"));
    }

    @Test
    void deleteDirectory_shouldSucceed() {
        Directory d = new Directory(); d.setDirId(10L); d.setParentDirId(5L);
        when(directoryMapper.selectDirectoryById(10L, 1L)).thenReturn(d);
        when(directoryMapper.selectDirectoriesByParentId(1L, 10L)).thenReturn(List.of());
        when(fileMapper.countFilesInDirectory(1L, 10L)).thenReturn(0);
        when(directoryMapper.deleteDirectory(10L, 1L)).thenReturn(1);

        assertTrue(directoryService.deleteDirectory(1L, 10L));
    }

    @Test
    void deleteDirectory_shouldThrow_whenRootDir() {
        Directory d = new Directory(); d.setDirId(10L); d.setParentDirId(null);
        when(directoryMapper.selectDirectoryById(10L, 1L)).thenReturn(d);
        assertThrows(RuntimeException.class, () -> directoryService.deleteDirectory(1L, 10L));
    }

    @Test
    void deleteDirectory_shouldThrow_whenHasChildren() {
        Directory d = new Directory(); d.setDirId(10L); d.setParentDirId(5L);
        when(directoryMapper.selectDirectoryById(10L, 1L)).thenReturn(d);
        when(directoryMapper.selectDirectoriesByParentId(1L, 10L)).thenReturn(List.of(new Directory()));
        assertThrows(RuntimeException.class, () -> directoryService.deleteDirectory(1L, 10L));
    }

    @Test
    void deleteDirectory_shouldThrow_whenHasFiles() {
        Directory d = new Directory(); d.setDirId(10L); d.setParentDirId(5L);
        when(directoryMapper.selectDirectoryById(10L, 1L)).thenReturn(d);
        when(directoryMapper.selectDirectoriesByParentId(1L, 10L)).thenReturn(List.of());
        when(fileMapper.countFilesInDirectory(1L, 10L)).thenReturn(3);
        assertThrows(RuntimeException.class, () -> directoryService.deleteDirectory(1L, 10L));
    }
}
