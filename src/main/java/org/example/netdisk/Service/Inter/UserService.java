package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_LoginDTO;
import org.example.netdisk.ResponseDTO.R_UserInfoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService {

    boolean register(String name, String password, String sex, MultipartFile avatar);

    Map<String, Object> login(String name, String password);

    R_UserInfoDTO getUserInfo(Long requesterId, Long userId);

    boolean changePassword(Long userId, String oldPassword, String newPassword);

    boolean updateUserInfo(Long userId, String newName, String newSex, MultipartFile newAvatar);
}
