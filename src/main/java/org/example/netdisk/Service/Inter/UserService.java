package org.example.netdisk.Service.Inter;

import org.example.PCOI.ResponseDTO.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {
    // 账号相关
    boolean register(String username, String password, Integer gender, List<R_SecurityIssue> SecurityIssues, MultipartFile avatar);
    Map<String,Object> login(String username, String password);
    boolean updatePassword(String tokenUsername,Integer type,String username, String newPassword);
    boolean changePassword(String userId, String oldPassword, String newPassword);
    boolean changeSecurityIssues(String userId, List<R_SecurityIssue> newSecurityIssues);
    // 作品和用户信息相关
    List<R_OverviewContribution> getContributionList(String userId);
    R_Audit_My_ContributionsDTO getMyContributions(String userId);
    List<R_User> getConcernedList(String userId);
    List<R_OverviewContribution> getLikedList(String userId);
    List<R_OverviewContribution> getFavouriteList(String userId);
    List<R_UserComment> getUserCommentList(String userId);
    boolean deleteComment(String commentId, String userId);
    boolean deleteContribution(String contributionId, String userId);
    boolean deletePendingContribution(String contributionId, String userId);
    boolean deleteDismissalContribution(String contributionId, String userId);
    R_UserInfoDTO getUserInfo(String requesterId,String userId);
    boolean updateUserInfo(String userId, String newUsername, Integer newGender, MultipartFile newAvatar);

    // 关注相关
    boolean concernUser(String userId, String concernedUserId);
    boolean unconcernUser(String userId, String concernedUserId);

    // 密保相关
    List<String> getMySecurityIssues(String username);
    R_VerifySecurityIssuesDTO verifySecurityIssues(String username, List<R_SecurityIssue> securityIssues);
}
