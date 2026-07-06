package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.PCOI.Entity.Tag;

import java.util.List;
@Mapper
public interface TagMapper {
    @Insert("INSERT INTO tag (tagName) VALUES (#{tagName})")
    void insertTag(String tagName);

    @Delete("DELETE FROM tag WHERE tagName = #{tagName}")
    void deleteTag(String tagName);

    @Select("SELECT EXISTS(SELECT 1 FROM tag WHERE tagName = #{tagName})")
    boolean isTagExist(String tagName);

    @Select("SELECT * FROM tag WHERE tagName = #{tagName}")
    Tag selectTagByName(String tagName);

    @Select("SELECT * FROM tag WHERE Id = #{tagId}")
    Tag selectTagById(int tagId);

    @Select("SELECT * FROM tag")
    List<Tag> selectAllTags();
}
