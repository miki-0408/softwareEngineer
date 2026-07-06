package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.StorageSpace;

@Mapper
public interface StorageSpaceMapper {

    @Insert("INSERT INTO storage_space (userId, totalSpace, usedSpace, remainSpace) VALUES (#{userId}, #{totalSpace}, #{usedSpace}, #{remainSpace})")
    void insertStorageSpace(StorageSpace storageSpace);

    @Select("SELECT * FROM storage_space WHERE userId = #{userId}")
    StorageSpace selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE storage_space SET usedSpace=#{usedSpace}, remainSpace=#{remainSpace} WHERE userId=#{userId}")
    int updateStorageSpace(StorageSpace storageSpace);
}
