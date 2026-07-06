package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.PCOI.Entity.Log;

import java.util.List;
@Mapper
public interface LogMapper {
    @Insert("INSERT INTO log (operatorId, description) VALUES (#{operatorId}, #{description})")
    void insertLog(@Param("operatorId") String operatorId, @Param("description") String description);

    @Select("SELECT * FROM log")
    List<Log> selectAllLogs();

}
