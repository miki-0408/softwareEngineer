package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
@Mapper
public interface LikeMapper {
    @Insert("INSERT INTO likes (userId, contributionId) VALUES (#{userId}, #{contributionId})")
    void insertLike(@Param("userId") String userId, @Param("contributionId") String contributionId);

    @Delete("DELETE FROM likes WHERE userId = #{userId} AND contributionId = #{contributionId}")
    void deleteLike(@Param("userId") String userId, @Param("contributionId") String contributionId);

    @Select("SELECT EXISTS(SELECT 1 FROM likes WHERE userId = #{userId} AND contributionId = #{contributionId})")
    boolean isLike(@Param("userId") String userId, @Param("contributionId") String contributionId);
}
