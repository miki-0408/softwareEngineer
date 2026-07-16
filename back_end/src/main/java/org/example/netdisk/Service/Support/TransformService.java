package org.example.netdisk.Service.Support;

import org.example.netdisk.Entity.*;
import org.example.netdisk.ResponseDTO.*;
import org.springframework.stereotype.Service;

import static org.example.netdisk.Service.Support.Enum.privateSpaceEnabled;

@Service
public class TransformService { // 数据转换服务：将Entity实体对象转换为前端ResponseDTO对象

    public R_User transformUserToRUser(User user) { // 将User实体转为R_User响应对象（隐藏密码等敏感字段）
        R_User rUser = new R_User();
        rUser.setUserId(String.valueOf(user.getUserId())); // Long转String便于前端处理
        rUser.setRole(user.getRole()); // 复制角色
        rUser.setName(user.getName()); // 复制用户名
        rUser.setSex(user.getSex()); // 复制性别
        rUser.setAvatar(user.getAvatar()); // 复制头像URL
        return rUser;
    }

    public R_StorageSpace transformStorageSpaceToRStorageSpace(StorageSpace storageSpace) { // 将StorageSpace实体转为响应对象
        R_StorageSpace rStorageSpace = new R_StorageSpace();
        rStorageSpace.setTotalSpace(storageSpace.getTotalSpace()); // 总空间
        rStorageSpace.setUsedSpace(storageSpace.getUsedSpace()); // 已用空间
        rStorageSpace.setRemainSpace(storageSpace.getRemainSpace()); // 剩余空间
        return rStorageSpace;
    }

    public R_PrivateSpace transformPrivateSpaceToRPrivateSpace(PrivateSpace privateSpace) { // 将PrivateSpace实体转为响应对象
        R_PrivateSpace rPrivateSpace = new R_PrivateSpace();
        if (privateSpace == null) { // 未创建私密空间记录则返回未启用状态
            rPrivateSpace.setEnabled(false); // 未启用
            return rPrivateSpace;
        }
        rPrivateSpace.setEnabled(privateSpace.getIsEncrypted() != null && privateSpace.getIsEncrypted() == privateSpaceEnabled); // 根据isEncrypted字段判断是否启用
        return rPrivateSpace;
    }

    public R_File transformFileToRFile(NetdiskFile file) { // 将NetdiskFile实体转为R_File响应对象
        R_File rFile = new R_File();
        rFile.setFileId(String.valueOf(file.getFileId())); // Long转String便于前端处理
        rFile.setFileName(file.getFileName()); // 复制文件名
        rFile.setFileType(file.getFileType()); // 复制文件类型
        rFile.setFileSize(file.getFileSize()); // 复制文件大小
        rFile.setUploadTime(file.getUploadTime()); // 复制上传时间
        rFile.setDirId(file.getDirId() == null ? null : String.valueOf(file.getDirId())); // 目录ID可能为null（根目录）
        rFile.setStatus(file.getStatus()); // 文件状态（正常/回收站）
        rFile.setIsEncrypted(file.getIsEncrypted()); // 加密状态
        rFile.setCompressMethod(file.getCompressMethod()); // 压缩方式
        return rFile;
    }

    public R_Directory transformDirectoryToRDirectory(Directory directory) { // 将Directory实体转为R_Directory响应对象
        R_Directory rDirectory = new R_Directory();
        rDirectory.setDirId(String.valueOf(directory.getDirId())); // Long转String
        rDirectory.setDirName(directory.getDirName()); // 复制目录名
        rDirectory.setParentDirId(directory.getParentDirId() == null ? null : String.valueOf(directory.getParentDirId())); // 父目录ID可能为null
        rDirectory.setCreateTime(directory.getCreateTime()); // 复制创建时间
        return rDirectory;
    }
}
