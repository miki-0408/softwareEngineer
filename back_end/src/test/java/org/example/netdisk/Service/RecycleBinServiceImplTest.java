package org.example.netdisk.Service;

import org.example.netdisk.Entity.Directory;
import org.example.netdisk.Entity.NetdiskFile;
import org.example.netdisk.Mapper.DirectoryMapper;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.Service.Impl.FileServiceImpl;
import org.example.netdisk.Service.Impl.PrivateSpaceServiceImpl;
import org.example.netdisk.Service.Impl.RecycleBinServiceImpl;
import org.example.netdisk.Service.Support.FileStorageService;
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
class RecycleBinServiceImplTest {

    @Mock private FileMapper fileMapper;
    @Mock private DirectoryMapper directoryMapper;
    @Mock private PrivateSpaceServiceImpl privateSpaceService;
    @Mock private FileServiceImpl fileService;
    @Mock private FileStorageService fileStorageService;
    @Mock private TransformService transformService;
    @InjectMocks private RecycleBinServiceImpl recycleBinService;

    @Test
    void listRecycleFiles_shouldReturnList() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L);
        when(fileMapper.selectFilesByStatus(1L, fileStatusRecycle)).thenReturn(List.of(f));
        R_File rf = new R_File(); rf.setFileId("1");
        when(transformService.transformFileToRFile(f)).thenReturn(rf);

        assertEquals(1, recycleBinService.listRecycleFiles(1L).size());
    }

    @Test
    void restoreFile_shouldReturnFalse_whenNotInRecycle() {
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(null);
        assertFalse(recycleBinService.restoreFile(1L, 1L));
    }

    @Test
    void restoreFile_shouldSucceed() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setFileName("test.txt");
        f.setDirId(10L); f.setStatus(fileStatusRecycle); f.setIsEncrypted(notEncrypted);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        Directory dir = new Directory(); dir.setDirId(10L);
        when(directoryMapper.selectDirectoryById(10L, 1L)).thenReturn(dir);
        when(fileService.uniqueFileName(1L, 10L, "test.txt")).thenReturn("test.txt");
        when(fileMapper.updateFileStatus(1L, 1L, fileStatusNormal)).thenReturn(1);

        assertTrue(recycleBinService.restoreFile(1L, 1L));
    }

    @Test
    void restoreFile_shouldRedirectToRoot_whenDirDeleted() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setFileName("test.txt");
        f.setDirId(99L); f.setStatus(fileStatusRecycle); f.setIsEncrypted(notEncrypted);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(directoryMapper.selectDirectoryById(99L, 1L)).thenReturn(null);
        Directory rootDir = new Directory(); rootDir.setDirId(1L);
        when(directoryMapper.selectRootDirectory(1L)).thenReturn(rootDir);
        when(fileService.uniqueFileName(1L, 1L, "test.txt")).thenReturn("test.txt");
        when(fileMapper.updateFile(any())).thenReturn(1);
        when(fileMapper.updateFileStatus(1L, 1L, fileStatusNormal)).thenReturn(1);

        assertTrue(recycleBinService.restoreFile(1L, 1L));
    }

    @Test
    void deletePermanently_shouldSucceed() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setPath("/path");
        f.setFileSize(100L); f.setStatus(fileStatusRecycle);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(fileMapper.deleteFile(1L, 1L)).thenReturn(1);

        assertTrue(recycleBinService.deletePermanently(1L, 1L));
        verify(fileStorageService).deleteStoredFile("/path");
    }

    @Test
    void deletePermanently_shouldReturnFalse_whenNotInRecycle() {
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(null);
        assertFalse(recycleBinService.deletePermanently(1L, 1L));
    }
}
