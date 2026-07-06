package org.example.netdisk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.netdisk.Mapper")
public class NetdiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetdiskApplication.class, args);
    }
}
