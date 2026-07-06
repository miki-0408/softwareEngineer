package org.example.netdisk.ResponseDTO;

import lombok.Data;

import java.util.List;

@Data
public class R_SearchDTO {
    private List<R_User> users;               // 用户列表
    private List<R_OverviewContribution> illustrations; // 插画列表
    private List<R_OverviewContribution> mangas;        // 漫画列表
}
