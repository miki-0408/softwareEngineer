package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagRelationMapper {
    @Insert("INSERT INTO tag_relation (contributionId, tagId) VALUES (#{contributionId}, #{tagId})")
    void insertTagRelation(@Param("contributionId") String contributionId, @Param("tagId") int tagId);

    @Select("SELECT tagId FROM tag_relation WHERE contributionId = #{contributionId}")
    List<Integer> getContributionTags(String contributionId);
}
