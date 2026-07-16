package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.netdisk.Entity.Log;

import java.util.List;

@Mapper // MyBatis映射器：负责日志表的数据库操作
public interface LogMapper {

    @Insert("INSERT INTO log (operatorId, description) VALUES (#{operatorId}, #{description})") // 插入操作日志记录
    void insertLog(@Param("operatorId") Long operatorId, @Param("description") String description);

    @Select("SELECT * FROM log ORDER BY time DESC LIMIT 200") // 查询最近200条日志（按时间倒序，最新在前）
    List<Log> selectAllLogs();
}
