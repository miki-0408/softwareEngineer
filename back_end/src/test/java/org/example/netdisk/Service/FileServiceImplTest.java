package org.example.netdisk.Service;

import org.example.netdisk.Entity.Directory;
import org.example.netdisk.Entity.NetdiskFile;
import org.example.netdisk.Entity.StorageSpace;
import org.example.netdisk.Mapper.DirectoryMapper;
import org.example.netdisk.Mapper.FileMapper;
import org.example.netdisk.Mapper.StorageSpaceMapper;
import org.example.netdisk.ResponseDTO.R_File;
import org.example.netdisk.Service.Impl.FileServiceImpl;
import org.example.netdisk.Service.Impl.PrivateSpaceServiceImpl;
import org.example.netdisk.Service.Support.FileConflictException;
import org.example.netdisk.Service.Support.FileStorageService;
import org.example.netdisk.Service.Support.TransformService;
import org.example.netdisk.Utils.EncryptionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.example.netdisk.Service.Support.Enum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock private FileMapper fileMapper;
    @Mock private DirectoryMapper directoryMapper;
    @Mock private StorageSpaceMapper storageSpaceMapper;
    @Mock private PrivateSpaceServiceImpl privateSpaceService;
    @Mock private FileStorageService fileStorageService;
    @Mock private TransformService transformService;
    @InjectMocks private FileServiceImpl fileService;

    // ==================== renameFile ====================
    @Test
    void renameFile_shouldSucceed() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setDirId(10L);
        f.setFileName("old.txt"); f.setStatus(fileStatusNormal);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(fileMapper.countFilesByName(1L, 10L, "new.txt")).thenReturn(0);
        when(fileMapper.updateFile(any())).thenReturn(1);

        assertTrue(fileService.renameFile(1L, 1L, "new.txt", false));
        verify(fileMapper).updateFile(any());
    }

    @Test
    void renameFile_shouldThrowConflict() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setDirId(10L);
        f.setFileName("old.txt"); f.setStatus(fileStatusNormal);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(fileMapper.countFilesByName(1L, 10L, "exist.txt")).thenReturn(1);

        assertThrows(FileConflictException.class,
            () -> fileService.renameFile(1L, 1L, "exist.txt", false));
    }

    @Test
    void renameFile_force_shouldReplace() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setDirId(10L);
        f.setFileName("old.txt"); f.setStatus(fileStatusNormal); f.setPath("/old");
        NetdiskFile conflict = new NetdiskFile(); conflict.setFileId(99L); conflict.setDirId(10L);
        conflict.setFileName("exist.txt"); conflict.setStatus(fileStatusNormal); conflict.setPath("/conflict");
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(fileMapper.countFilesByName(1L, 10L, "exist.txt")).thenReturn(1);
        when(fileMapper.selectFilesByNameAndDir(1L, 10L, "exist.txt", fileStatusNormal))
            .thenReturn(List.of(conflict));
        when(fileMapper.updateFile(any())).thenReturn(1);

        assertTrue(fileService.renameFile(1L, 1L, "exist.txt", true));
        verify(fileStorageService).deleteStoredFile("/conflict");
        verify(fileStorageService).deleteStoredFile(anyString());
    }

    // ==================== moveToRecycleBin ====================
    @Test
    void moveToRecycleBin_shouldSucceed() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setStatus(fileStatusNormal);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(fileMapper.updateFileStatus(1L, 1L, fileStatusRecycle)).thenReturn(1);

        assertTrue(fileService.moveToRecycleBin(1L, 1L));
    }

    @Test
    void moveToRecycleBin_shouldReturnFalse_whenNotFound() {
        when(fileMapper.selectFileById(99L, 1L)).thenReturn(null);
        assertFalse(fileService.moveToRecycleBin(1L, 99L));
    }

    // ==================== listFiles ====================
    @Test
    void listFiles_shouldReturnList() {
        Directory dir = new Directory(); dir.setDirId(10L);
        when(directoryMapper.selectDirectoryById(10L, 1L)).thenReturn(dir);
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L);
        when(fileMapper.selectFilesByDirAndStatus(1L, 10L, fileStatusNormal)).thenReturn(List.of(f));
        R_File rf = new R_File(); rf.setFileId("1");
        when(transformService.transformFileToRFile(f)).thenReturn(rf);

        List<R_File> result = fileService.listFiles(1L, 10L);
        assertEquals(1, result.size());
    }

    @Test
    void listFiles_shouldThrow_whenDirNotFound() {
        when(directoryMapper.selectDirectoryById(10L, 1L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> fileService.listFiles(1L, 10L));
    }

    // ==================== encryptFile / decryptFile ====================
    @Test
    void encryptFile_shouldEncryptAndMove() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setDirId(10L);
        f.setFileName("test.txt"); f.setStatus(fileStatusNormal);
        f.setIsEncrypted(notEncrypted); f.setFileSize(100L); f.setPath("/old");
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(fileMapper.countFilesByName(1L, 20L, "test.txt")).thenReturn(0);
        when(fileStorageService.readStoredFile("/old")).thenReturn("compressed".getBytes());
        when(fileStorageService.saveCompressedFile(any(), eq(1L), eq(1L))).thenReturn("/new");
        StorageSpace sp = new StorageSpace(); sp.setUsedSpace(100L); sp.setTotalSpace(1000L); sp.setRemainSpace(900L);
        when(storageSpaceMapper.selectByUserId(1L)).thenReturn(sp);
        when(fileMapper.updateFile(any())).thenReturn(1);

        assertTrue(fileService.encryptFile(1L, 1L, "pwd", 20L, false));
        verify(privateSpaceService).validatePrivatePassword(1L, "pwd");
    }

    @Test
    void encryptFile_shouldReturnTrue_whenAlreadyEncrypted() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setStatus(fileStatusNormal);
        f.setIsEncrypted(encrypted);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);

        assertTrue(fileService.encryptFile(1L, 1L, "pwd", 20L, false));
        verify(fileMapper, never()).updateFile(any());
    }

    @Test
    void decryptFile_shouldDecryptAndMove() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setDirId(20L);
        f.setFileName("test.txt"); f.setStatus(fileStatusNormal);
        f.setIsEncrypted(encrypted); f.setFileSize(108L); f.setPath("/encrypted");
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(fileMapper.countFilesByName(1L, 10L, "test.txt")).thenReturn(0);
        when(fileStorageService.readStoredFile("/encrypted")).thenReturn("encrypted_data".getBytes());
        when(fileStorageService.saveCompressedFile(any(), eq(1L), eq(1L))).thenReturn("/new");
        StorageSpace sp = new StorageSpace(); sp.setUsedSpace(100L); sp.setTotalSpace(1000L); sp.setRemainSpace(900L);
        when(storageSpaceMapper.selectByUserId(1L)).thenReturn(sp);
        when(fileMapper.updateFile(any())).thenReturn(1);

        assertTrue(fileService.decryptFile(1L, 1L, "pwd", 10L, false));
        verify(privateSpaceService).validatePrivatePassword(1L, "pwd");
    }

    // ==================== moveFile ====================
    @Test
    void moveFile_shouldSucceed() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setDirId(10L);
        f.setFileName("test.txt"); f.setStatus(fileStatusNormal);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        Directory targetDir = new Directory(); targetDir.setDirId(20L);
        when(directoryMapper.selectDirectoryById(20L, 1L)).thenReturn(targetDir);
        when(fileMapper.countFilesByName(1L, 20L, "test.txt")).thenReturn(0);
        when(fileMapper.updateFile(any())).thenReturn(1);

        assertTrue(fileService.moveFile(1L, 1L, 20L, false));
    }

    @Test
    void moveFile_shouldReturnFalse_whenTargetDirNotFound() {
        NetdiskFile f = new NetdiskFile(); f.setFileId(1L); f.setStatus(fileStatusNormal);
        when(fileMapper.selectFileById(1L, 1L)).thenReturn(f);
        when(directoryMapper.selectDirectoryById(20L, 1L)).thenReturn(null);

        assertFalse(fileService.moveFile(1L, 1L, 20L, false));
    }

    // ==================== uniqueFileName ====================
    @Test
    void uniqueFileName_shouldAddSuffix_whenConflict() {
        when(fileMapper.countFilesByName(1L, 1L, "test.txt")).thenReturn(1);
        when(fileMapper.countFilesByName(1L, 1L, "test (1).txt")).thenReturn(0);

        assertEquals("test (1).txt", fileService.uniqueFileName(1L, 1L, "test.txt"));
    }

    @Test
    void uniqueFileName_shouldReturnOriginal_whenNoConflict() {
        when(fileMapper.countFilesByName(1L, 1L, "unique.txt")).thenReturn(0);
        assertEquals("unique.txt", fileService.uniqueFileName(1L, 1L, "unique.txt"));
    }
}
