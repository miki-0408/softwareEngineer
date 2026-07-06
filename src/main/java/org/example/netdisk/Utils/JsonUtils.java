package org.example.netdisk.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

// 添加在测试类中
public class JsonUtils {
    public static String toJson(Object obj) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}