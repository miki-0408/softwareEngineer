package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data // Lombok自动生成getter/setter等
public class R_LoginDTO { // 登录响应DTO：包含用户信息和JWT令牌
    private R_User user; // 用户信息
    private String token; // JWT令牌，后续请求携带此令牌进行身份验证
}
