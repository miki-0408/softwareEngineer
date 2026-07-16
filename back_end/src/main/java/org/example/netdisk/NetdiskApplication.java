package org.example.netdisk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // Spring Boot核心注解：启用自动配置、组件扫描等
@MapperScan("org.example.netdisk.Mapper") // 扫描MyBatis Mapper接口所在的包
public class NetdiskApplication { // Spring Boot应用主入口类

    public static void main(String[] args) { // 应用启动入口
        SpringApplication.run(NetdiskApplication.class, args); // 启动Spring Boot应用
    }
}
