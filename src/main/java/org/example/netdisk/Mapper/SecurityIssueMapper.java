package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.example.PCOI.Entity.SecurityIssue;

import java.util.List;
@Mapper
public interface SecurityIssueMapper {

    @Insert("INSERT INTO security_issue (userId, description, answer) VALUES (#{userId}, #{description}, #{answer})")
    void insertSecurityIssue(SecurityIssue securityIssue);

    @Select("SELECT * FROM security_issue WHERE userId = #{userId}")
    List<SecurityIssue> selectSecurityIssuesByUserId(String userId);

    @Delete("DELETE FROM security_issue WHERE userId = #{userId}")
    void deleteSecurityIssuesByUserId(String userId);

}
