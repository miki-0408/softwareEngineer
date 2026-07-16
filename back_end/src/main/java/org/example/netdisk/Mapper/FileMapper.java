package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.NetdiskFile;

import java.util.List;

@Mapper // MyBatis映射器：负责文件表的数据库操作
public interface FileMapper {

    @Insert("""
            INSERT INTO file (fileName, fileType, fileSize, path, userId, dirId, status, isEncrypted, compressMethod)
            VALUES (#{fileName}, #{fileType}, #{fileSize}, #{path}, #{userId}, #{dirId}, #{status}, #{isEncrypted}, #{compressMethod})
            """) // 插入新文件记录（文本块SQL提高可读性）
    @Options(useGeneratedKeys = true, keyProperty = "fileId") // 自动回填自增主键到对象的fileId属性
    void insertFile(NetdiskFile file);

    @Update("UPDATE file SET fileName=#{fileName}, fileType=#{fileType}, fileSize=#{fileSize}, path=#{path}, dirId=#{dirId}, status=#{status}, isEncrypted=#{isEncrypted}, compressMethod=#{compressMethod} WHERE fileId=#{fileId} AND userId=#{userId}") // 完整更新文件所有字段（如替换上传场景）
    int updateFile(NetdiskFile file);

    @Update("UPDATE file SET status=#{status} WHERE fileId=#{fileId} AND userId=#{userId}") // 仅更新文件状态（移入/移出回收站）
    int updateFileStatus(@Param("fileId") Long fileId, @Param("userId") Long userId, @Param("status") Integer status);

    @Delete("DELETE FROM file WHERE fileId=#{fileId} AND userId=#{userId}") // 物理删除文件记录（永久删除）
    int deleteFile(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Select("SELECT * FROM file WHERE fileId = #{fileId} AND userId = #{userId}") // 根据文件ID和用户ID查询单个文件
    NetdiskFile selectFileById(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Select("SELECT * FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = #{status} AND isEncrypted = 0") // 查询普通文件列表（排除加密文件，这些属于私密空间）
    List<NetdiskFile> selectFilesByDirAndStatus(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("status") Integer status);

    @Select("SELECT * FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = #{status} AND isEncrypted = 1") // 查询私密空间文件列表（仅加密文件）
    List<NetdiskFile> selectPrivateFilesByDirAndStatus(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("status") Integer status);

    @Select("SELECT * FROM file WHERE userId = #{userId} AND status = #{status}") // 按状态查询用户文件（如回收站列表 status=1）
    List<NetdiskFile> selectFilesByStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT * FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND fileName = #{fileName} AND status = #{status}") // 按目录和文件名查找文件，用于冲突检测
    List<NetdiskFile> selectFilesByNameAndDir(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("fileName") String fileName, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = 0 AND isEncrypted = 0") // 统计某目录下普通文件数量（正常状态且未加密）
    int countFilesInDirectory(@Param("userId") Long userId, @Param("dirId") Long dirId);

    @Select("SELECT COUNT(*) FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND status = 0 AND isEncrypted = 1") // 统计私密空间某目录下文件数量
    int countPrivateFilesInDirectory(@Param("userId") Long userId, @Param("dirId") Long dirId);

    @Select("SELECT COUNT(*) FROM file WHERE userId = #{userId} AND dirId = #{dirId} AND fileName = #{fileName} AND status = 0") // 检查某目录下是否存在同名活动文件，用于重名检测
    int countFilesByName(@Param("userId") Long userId, @Param("dirId") Long dirId, @Param("fileName") String fileName);
}
