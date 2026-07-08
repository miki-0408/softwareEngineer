package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.Directory;

import java.util.List;

@Mapper
public interface DirectoryMapper {

    @Insert("INSERT INTO directory (dirName, parentDirId, userId) VALUES (#{dirName}, #{parentDirId}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "dirId")
    void insertDirectory(Directory directory);

    @Update("UPDATE directory SET dirName=#{dirName} WHERE dirId=#{dirId} AND userId=#{userId}")
    int updateDirectoryName(Directory directory);

    @Delete("DELETE FROM directory WHERE dirId=#{dirId} AND userId=#{userId}")
    int deleteDirectory(@Param("dirId") Long dirId, @Param("userId") Long userId);

    @Select("SELECT * FROM directory WHERE dirId = #{dirId} AND userId = #{userId}")
    Directory selectDirectoryById(@Param("dirId") Long dirId, @Param("userId") Long userId);

    @Select("SELECT * FROM directory WHERE userId = #{userId} AND parentDirId IS NULL LIMIT 1")
    Directory selectRootDirectory(@Param("userId") Long userId);

    @Select("SELECT * FROM directory WHERE userId = #{userId} AND parentDirId = #{parentDirId}")
    List<Directory> selectDirectoriesByParentId(@Param("userId") Long userId, @Param("parentDirId") Long parentDirId);

    @Select("SELECT * FROM directory WHERE userId = #{userId} AND parentDirId IS NULL")
    List<Directory> selectRootDirectories(@Param("userId") Long userId);

    /** 检查父目录下是否存在同名子目录 */
    @Select("SELECT COUNT(*) FROM directory WHERE userId = #{userId} AND parentDirId = #{parentDirId} AND dirName = #{dirName}")
    int countDirsByName(@Param("userId") Long userId, @Param("parentDirId") Long parentDirId, @Param("dirName") String dirName);
}
