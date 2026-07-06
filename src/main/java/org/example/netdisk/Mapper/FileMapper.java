package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.NetdiskFile;

import java.util.List;

@Mapper
public interface FileMapper {

    @Insert("""
            INSERT INTO file (fileName, fileType, fileSize, path, userId, dirId, status, isEncrypted)
            VALUES (#{fileName}, #{fileType}, #{fileSize}, #{path}, #{userId}, #{dirId}, #{status}, #{isEncrypted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "fileId")
    void insertFile(NetdiskFile file);

    @Update("UPDATE file SET fileName=#{fileName}, fileType=#{fileType}, fileSize=#{fileSize}, path=#{path}, dirId=#{dirId}, status=#{status}, isEncrypted=#{isEncrypted} WHERE fileId=#{fileId} AND userId=#{userId}")
    int updateFile(NetdiskFile file);

    @Update("UPDATE file SET status=#{status} WHERE fileId=#{fileId} AND userId=#{userId}")
    int updateFileStatus(@Param("fileId") Long fileId, @Param("userId") Long userId, @Param("status") Integer status);

    @Delete("DELETE FROM file WHERE fileId=#{fileId} AND userId=#{userId}")
    int deleteFile(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Select("SELECT * FROM file WHERE fileId = #{fileId} AND userId = #{userId}")
    NetdiskFile selectFileById(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Select("SELECT * FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = #{status}")
    List<NetdiskFile> selectFilesByDirAndStatus(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("status") Integer status);

    @Select("SELECT * FROM file WHERE userId = #{userId} AND status = #{status}")
    List<NetdiskFile> selectFilesByStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = 0")
    int countFilesInDirectory(@Param("userId") Long userId, @Param("dirId") Long dirId);
}
