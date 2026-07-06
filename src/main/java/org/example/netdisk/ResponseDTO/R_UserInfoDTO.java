package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data
public class R_UserInfoDTO {
    private R_User user;               // 用户信息
    private Boolean isConcerned;        // 是否已关注
}
