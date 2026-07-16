package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.Directory;

import java.util.List;

@Mapper // MyBatis映射器：负责目录表的数据库操作
public interface DirectoryMapper {

    @Insert("INSERT INTO directory (dirName, parentDirId, userId) VALUES (#{dirName}, #{parentDirId}, #{userId})") // 插入新目录记录
    @Options(useGeneratedKeys = true, keyProperty = "dirId") // 自动回填自增主键到对象的dirId属性
    void insertDirectory(Directory directory);

    @Update("UPDATE directory SET dirName=#{dirName} WHERE dirId=#{dirId} AND userId=#{userId}") // 更新目录名称，限制用户只能修改自己的目录
    int updateDirectoryName(Directory directory);

    @Delete("DELETE FROM directory WHERE dirId=#{dirId} AND userId=#{userId}") // 删除指定目录，限制用户只能删除自己的目录
    int deleteDirectory(@Param("dirId") Long dirId, @Param("userId") Long userId);

    @Select("SELECT * FROM directory WHERE dirId = #{dirId} AND userId = #{userId}") // 根据目录ID和用户ID查询目录详情
    Directory selectDirectoryById(@Param("dirId") Long dirId, @Param("userId") Long userId);

    @Select("SELECT * FROM directory WHERE userId = #{userId} AND parentDirId IS NULL LIMIT 1") // 查询用户的根目录（parentDirId为NULL）
    Directory selectRootDirectory(@Param("userId") Long userId);

    @Select("SELECT * FROM directory WHERE userId = #{userId} AND parentDirId = #{parentDirId}") // 查询指定父目录下的所有子目录
    List<Directory> selectDirectoriesByParentId(@Param("userId") Long userId, @Param("parentDirId") Long parentDirId);

    @Select("SELECT * FROM directory WHERE userId = #{userId} AND parentDirId IS NULL") // 查询用户的所有根目录列表
    List<Directory> selectRootDirectories(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM directory WHERE userId = #{userId} AND parentDirId = #{parentDirId} AND dirName = #{dirName}") // 统计父目录下同名子目录数量，用于重名检查
    int countDirsByName(@Param("userId") Long userId, @Param("parentDirId") Long parentDirId, @Param("dirName") String dirName);
}
