package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.NetdiskFile;

import java.util.List;

@Mapper
public interface FileMapper {

    @Insert("""
            INSERT INTO file (fileName, fileType, fileSize, path, userId, dirId, status, isEncrypted, compressMethod)
            VALUES (#{fileName}, #{fileType}, #{fileSize}, #{path}, #{userId}, #{dirId}, #{status}, #{isEncrypted}, #{compressMethod})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "fileId")
    void insertFile(NetdiskFile file);

    @Update("UPDATE file SET fileName=#{fileName}, fileType=#{fileType}, fileSize=#{fileSize}, path=#{path}, dirId=#{dirId}, status=#{status}, isEncrypted=#{isEncrypted}, compressMethod=#{compressMethod} WHERE fileId=#{fileId} AND userId=#{userId}")
    int updateFile(NetdiskFile file);

    @Update("UPDATE file SET status=#{status} WHERE fileId=#{fileId} AND userId=#{userId}")
    int updateFileStatus(@Param("fileId") Long fileId, @Param("userId") Long userId, @Param("status") Integer status);

    @Delete("DELETE FROM file WHERE fileId=#{fileId} AND userId=#{userId}")
    int deleteFile(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Select("SELECT * FROM file WHERE fileId = #{fileId} AND userId = #{userId}")
    NetdiskFile selectFileById(@Param("fileId") Long fileId, @Param("userId") Long userId);

    /** 普通文件列表：排除已加密（已在私密空间）的文件 */
    @Select("SELECT * FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = #{status} AND isEncrypted = 0")
    List<NetdiskFile> selectFilesByDirAndStatus(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("status") Integer status);

    /** 私密空间文件列表 */
    @Select("SELECT * FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = #{status} AND isEncrypted = 1")
    List<NetdiskFile> selectPrivateFilesByDirAndStatus(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("status") Integer status);

    @Select("SELECT * FROM file WHERE userId = #{userId} AND status = #{status}")
    List<NetdiskFile> selectFilesByStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /** 按目录和文件名查找文件（不限 isEncrypted，用于冲突检查等） */
    @Select("SELECT * FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND fileName = #{fileName} AND status = #{status}")
    List<NetdiskFile> selectFilesByNameAndDir(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("fileName") String fileName, @Param("status") Integer status);

    /** 统计某目录下普通文件数 */
    @Select("SELECT COUNT(*) FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = 0 AND isEncrypted = 0")
    int countFilesInDirectory(@Param("userId") Long userId, @Param("dirId") Long dirId);

    /** 统计私密空间某目录下文件数 */
    @Select("SELECT COUNT(*) FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = 0 AND isEncrypted = 1")
    int countPrivateFilesInDirectory(@Param("userId") Long userId, @Param("dirId") Long dirId);

    /** 检查某目录下是否存在同名文件 */
    @Select("SELECT COUNT(*) FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND fileName = #{fileName} AND status = 0")
    int countFilesByName(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("fileName") String fileName);
}
