package org.example.netdisk.Service.Inter;

import org.example.PCOI.Entity.Log;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SystemAdminService {
    boolean updateUserInfo(String userId, String newUsername, Integer newGender, MultipartFile newAvatar);
    boolean resetPassword(String userId);
    List<Log> getLogs();
}

