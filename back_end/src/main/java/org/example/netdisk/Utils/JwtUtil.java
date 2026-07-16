package org.example.netdisk.Utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String KEY = "mayun"; // JWT签名密钥，生产环境应从配置中读取

    public static String genToken(Map<String, Object> claims) { // 接收业务数据生成JWT令牌，有效期12小时
        return JWT.create()
                .withClaim("claims",claims) // 将业务数据（如userId、role）存入载荷
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12)) // 设置过期时间为当前时间加12小时
                .sign(Algorithm.HMAC256(KEY)); // 使用HMAC-SHA256算法签名
    }

    public static Map<String,Object> parseToken(String token) { // 接收令牌验证签名并提取载荷中的业务数据
        return JWT.require(Algorithm.HMAC256(KEY)) // 指定验证算法
                .build()
                .verify(token) // 验证令牌签名和有效期
                .getClaim("claims") // 获取"claims"字段
                .asMap() // 转为Map返回
                ;
    }

}
