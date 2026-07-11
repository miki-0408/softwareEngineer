package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.netdisk.Entity.PrivateSpace;

@Mapper
public interface PrivateSpaceMapper {

    @Insert("INSERT INTO private_space (userId, password, isEncrypted) VALUES (#{userId}, #{password}, #{isEncrypted})")
    void insertPrivateSpace(PrivateSpace privateSpace);

    @Select("SELECT * FROM private_space WHERE userId = #{userId}")
    PrivateSpace selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE private_space SET password=#{password}, isEncrypted=#{isEncrypted} WHERE userId=#{userId}")
    int updatePrivateSpace(PrivateSpace privateSpace);

}
