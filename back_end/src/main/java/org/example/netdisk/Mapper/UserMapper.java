package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.User;

@Mapper // MyBatis映射器：负责用户表的数据库操作
public interface UserMapper {

    @Insert("INSERT INTO user (name, password, sex, role) VALUES (#{name}, #{password}, #{sex}, #{role})") // 插入新用户记录（注册时调用）
    @Options(useGeneratedKeys = true, keyProperty = "userId") // 自动回填自增主键到对象的userId属性
    void insertUser(User user);

    @Update("UPDATE user SET name=#{name}, password=#{password}, avatar=#{avatar}, sex=#{sex}, role=#{role} WHERE userId=#{userId}") // 完整更新用户信息（支持更新所有字段）
    void updateUser(User user);

    @Select("SELECT * FROM user WHERE userId = #{userId}") // 根据用户ID查询用户信息
    User selectUserById(@Param("userId") Long userId);

    @Select("SELECT * FROM user WHERE name = #{name}") // 根据用户名查询用户（登录、重名检查时使用）
    User selectUserByName(@Param("name") String name);
}
