package org.example.netdisk.Service.Support;

import org.example.netdisk.Entity.*;
import org.example.netdisk.ResponseDTO.*;
import org.springframework.stereotype.Service;

import static org.example.netdisk.Service.Support.Enum.privateSpaceEnabled;

@Service
public class TransformService {

    public R_User transformUserToRUser(User user) {
        R_User rUser = new R_User();
        rUser.setUserId(String.valueOf(user.getUserId()));
        rUser.setRole(user.getRole());
        rUser.setName(user.getName());
        rUser.setSex(user.getSex());
        rUser.setAvatar(user.getAvatar());
        return rUser;
    }

    public R_StorageSpace transformStorageSpaceToRStorageSpace(StorageSpace storageSpace) {
        R_StorageSpace rStorageSpace = new R_StorageSpace();
        rStorageSpace.setTotalSpace(storageSpace.getTotalSpace());
        rStorageSpace.setUsedSpace(storageSpace.getUsedSpace());
        rStorageSpace.setRemainSpace(storageSpace.getRemainSpace());
        return rStorageSpace;
    }

    public R_PrivateSpace transformPrivateSpaceToRPrivateSpace(PrivateSpace privateSpace) {
        R_PrivateSpace rPrivateSpace = new R_PrivateSpace();
        if (privateSpace == null) {
            rPrivateSpace.setEnabled(false);
            return rPrivateSpace;
        }
        rPrivateSpace.setEnabled(privateSpace.getIsEncrypted() != null && privateSpace.getIsEncrypted() == privateSpaceEnabled);
        return rPrivateSpace;
    }

    public R_File transformFileToRFile(NetdiskFile file) {
        R_File rFile = new R_File();
        rFile.setFileId(String.valueOf(file.getFileId()));
        rFile.setFileName(file.getFileName());
        rFile.setFileType(file.getFileType());
        rFile.setFileSize(file.getFileSize());
        rFile.setUploadTime(file.getUploadTime());
        rFile.setDirId(file.getDirId() == null ? null : String.valueOf(file.getDirId()));
        rFile.setStatus(file.getStatus());
        rFile.setIsEncrypted(file.getIsEncrypted());
        return rFile;
    }

    public R_Directory transformDirectoryToRDirectory(Directory directory) {
        R_Directory rDirectory = new R_Directory();
        rDirectory.setDirId(String.valueOf(directory.getDirId()));
        rDirectory.setDirName(directory.getDirName());
        rDirectory.setParentDirId(directory.getParentDirId() == null ? null : String.valueOf(directory.getParentDirId()));
        rDirectory.setCreateTime(directory.getCreateTime());
        return rDirectory;
    }
}
