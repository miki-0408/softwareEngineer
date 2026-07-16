package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.StorageSpace;

@Mapper // MyBatis映射器：负责存储空间表的数据库操作
public interface StorageSpaceMapper {

    @Insert("INSERT INTO storage_space (userId, totalSpace, usedSpace, remainSpace) VALUES (#{userId}, #{totalSpace}, #{usedSpace}, #{remainSpace})") // 为用户创建存储空间记录（注册时调用）
    void insertStorageSpace(StorageSpace storageSpace);

    @Select("SELECT * FROM storage_space WHERE userId = #{userId}") // 根据用户ID查询存储空间信息
    StorageSpace selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE storage_space SET usedSpace=#{usedSpace}, remainSpace=#{remainSpace} WHERE userId=#{userId}") // 更新已用空间和剩余空间（上传/删除文件时同步）
    int updateStorageSpace(StorageSpace storageSpace);
}
