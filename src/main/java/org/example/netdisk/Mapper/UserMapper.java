package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.User;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO user (name, password, sex, role) VALUES (#{name}, #{password}, #{sex}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    void insertUser(User user);

    @Update("UPDATE user SET name=#{name}, password=#{password}, avatar=#{avatar}, sex=#{sex}, role=#{role} WHERE userId=#{userId}")
    void updateUser(User user);

    @Select("SELECT * FROM user WHERE userId = #{userId}")
    User selectUserById(@Param("userId") Long userId);

    @Select("SELECT * FROM user WHERE name = #{name}")
    User selectUserByName(@Param("name") String name);
}
