package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
@Mapper
public interface FollowMapper {
    @Insert("INSERT INTO follow (followerId, followedId) VALUES (#{followerId}, #{followedId})")
    void insertFollow(@Param("followerId") String followerId, @Param("followedId") String followedId); //followerId关注followedId 即follower是粉丝,followed是被关注者

    @Delete("DELETE FROM follow WHERE followerId = #{followerId} AND followedId = #{followedId}") //followerId关注followedId 即follower是粉丝,followed是被关注者
    void deleteFollow(@Param("followerId") String followerId, @Param("followedId") String followedId);

    @Select("SELECT EXISTS(SELECT 1 FROM follow WHERE followerId = #{followerId} AND followedId = #{followedId})")
    boolean isFollow(@Param("followerId") String followerId, @Param("followedId") String followedId);
}
