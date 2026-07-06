package org.example.netdisk.Service.Inter;

import org.example.netdisk.ResponseDTO.R_PrivateSpace;
import org.example.netdisk.ResponseDTO.R_VerifyPrivateSpaceDTO;

public interface PrivateSpaceService {

    boolean enablePrivateSpace(Long userId, String password);

    boolean disablePrivateSpace(Long userId, String password);

    R_VerifyPrivateSpaceDTO verifyPrivateSpace(Long userId, String password);

    R_PrivateSpace getPrivateSpaceStatus(Long userId);
}
