package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data
public class R_UserInfoDTO {
    private R_User user;
    private R_StorageSpace storage;
    private R_PrivateSpace privateSpace;
}
