package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.PrivateSpace;

@Mapper // MyBatis映射器：负责私密空间表的数据库操作
public interface PrivateSpaceMapper {

    @Insert("INSERT INTO private_space (userId, password, isEncrypted) VALUES (#{userId}, #{password}, #{isEncrypted})") // 创建私密空间记录（首次启用时插入）
    void insertPrivateSpace(PrivateSpace privateSpace);

    @Select("SELECT * FROM private_space WHERE userId = #{userId}") // 根据用户ID查询私密空间信息
    PrivateSpace selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE private_space SET password=#{password}, isEncrypted=#{isEncrypted} WHERE userId=#{userId}") // 更新私密空间密码和启用状态
    int updatePrivateSpace(PrivateSpace privateSpace);

}
