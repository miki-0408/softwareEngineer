package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.PCOI.Entity.User;

import java.util.List;
@Mapper
public interface UserMapper {
    @Insert("INSERT INTO user (username, password, avatar, sex) VALUES (#{username},#{password},#{avatar},#{sex})")
    void insertUser(User user);

    @Update("UPDATE user SET username=#{username}, password=#{password}, avatar=#{avatar},sex =#{sex} ,status =#{status}, userId=#{userId} WHERE userId = #{userId}")
    void updateUser(User user);

    @Select("SELECT * FROM user WHERE userId = #{userId}")
    User selectUserById(String userId);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectUserByName(String username);

    @Select("""
            SELECT u.*
            FROM user u
            JOIN follow f ON u.userId = f.followerId
            WHERE f.followedId = #{userId}
         """)
    List<User> selectFollowerUsersByUserId(String userId);

    @Select("""
            SELECT *
            FROM user
            WHERE username LIKE CONCAT('%', #{keyword}, '%')  -- 核心：子串匹配
            LIMIT #{limit}
        """)
    List<User> selectUsersByName(
            @Param("limit") int limit,
            @Param("keyword") String keyword
    );

    @Select("SELECT * FROM user WHERE status = #{status}")
    List<User> selectUsersByStatus(@Param("status") int status);

    @Select("""
            SELECT u.*
            FROM user u
            JOIN follow f ON u.userId = f.followedId
            WHERE f.followerId = #{userId}
    """)
    List<User> selectFollowedUsersByUserId(String userId);
}
