package org.example.netdisk.Service.Inter;

import org.example.netdisk.Entity.Log;

import java.util.List;

public interface SystemAdminService {

    boolean updateUserInfo(Long userId, String newName, String newSex, org.springframework.web.multipart.MultipartFile newAvatar);

    boolean resetPassword(Long userId);

    List<Log> getLogs();
}
