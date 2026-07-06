package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.PCOI.Entity.*;
import org.example.PCOI.Mapper.*;
import org.example.PCOI.ResponseDTO.*;
import org.example.PCOI.Service.Inter.UserService;
import org.example.PCOI.Service.Support.FileStorageService;
import org.example.PCOI.Service.Support.TransformService;
import org.example.PCOI.Utils.BcryptUtil;
import org.example.PCOI.Utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.example.PCOI.Service.Support.Enum.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper usermapper;
    @Autowired
    private SecurityIssueMapper securityissuemapper;
    @Autowired
    private ContributionMapper contributionmapper;
    @Autowired
    private CommentMapper commentmapper;
    @Autowired
    private FollowMapper followmapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TransformService transformService;

    @Override
    public boolean register(String username, String password, Integer gender, List<R_SecurityIssue> securityIssues, MultipartFile avatar) {
        String contentType = avatar.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new RuntimeException("仅支持图片类型文件");
        }
        try {
            if (usermapper.selectUserByName(username) != null) {
                return false; // 用户名已存在
            }
            User user = new User();
            user.setUsername(username);
            user.setPassword(BcryptUtil.hash(password));
            user.setSex(gender);
            user.setRole(normalUser);
            user.setStatus(normal);
            usermapper.insertUser(user);
            user = usermapper.selectUserByName(username); // 获取插入后的用户以获取其 ID
            // 仅当上传了头像时才覆盖数据库默认头像 URL
            String avatarUrl = fileStorageService.saveAvatar(avatar, user.getUserId());
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                user.setAvatar(avatarUrl);
                usermapper.updateUser(user);
            }
            for (R_SecurityIssue issue : securityIssues) {
                SecurityIssue securityIssue = transformService.transformRSecurityIssueToSecurityIssue(issue, user.getUserId());
                securityissuemapper.insertSecurityIssue(securityIssue);
            }
            return true;
        }catch(RuntimeException e){
            throw new RuntimeException("仅支持图片类型文件", e);
        }
    }

    @Override
    public Map<String,Object> login(String username, String password) {
        User user = usermapper.selectUserByName(username);
        if(user == null) {
            return Map.of( "rLoginDTO","null",
                    "message", "用户不存在");
        }
        if(!BcryptUtil.matches(password, user.getPassword()))
        {
            return Map.of( "rLoginDTO", "null",
                    "message", "密码错误");
        }
        if(user.getStatus().equals(banned))
        {
            return Map.of( "rLoginDTO", "null",
                    "message", "用户已被封禁");
        }
        Claims claims = new Claims(user.getUsername(), user.getUserId(),user.getRole(),login);
        String token = JwtUtil.genToken(claims.toMap());
        R_User rUser = transformService.transformUserToRUser(user);
        R_LoginDTO loginDTO = new R_LoginDTO();
        loginDTO.setUser(rUser);
        loginDTO.setToken(token);
        return Map.of( "rLoginDTO", loginDTO,
                "message", "登录成功");
    }

    @Override
    public boolean updatePassword(String tokenUsername, Integer type, String username, String newPassword) {
        if(!tokenUsername.equals(username)||!type.equals(updatePWD)) {
            return false; // 鉴权失败
        }
        User user = usermapper.selectUserByName(username);
        if(user == null) {
            return false; // 用户不存在
        }
        user.setPassword(BcryptUtil.hash(newPassword));
        usermapper.updateUser(user);
        return true;
    }

    @Override
    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        User user = usermapper.selectUserById(userId);
        if(user == null) {
            return false; // 用户不存在
        }
        if(!BcryptUtil.matches(oldPassword, user.getPassword())) {
            return false; // 旧密码错误
        }
        user.setPassword(BcryptUtil.hash(newPassword));
        usermapper.updateUser(user);
        return true;
    }

    @Override
    public boolean changeSecurityIssues(String userId, List<R_SecurityIssue> newSecurityIssues) {
        User user = usermapper.selectUserById(userId);
        if(user == null) {
            return false; // 用户不存在
        }
        securityissuemapper.deleteSecurityIssuesByUserId(userId);
        for(R_SecurityIssue rIssue : newSecurityIssues) {
            SecurityIssue securityIssue = transformService.transformRSecurityIssueToSecurityIssue(rIssue, userId);
            securityissuemapper.insertSecurityIssue(securityIssue);
        }
        return true;
    }

    @Override
    public List<R_OverviewContribution> getContributionList(String userId) {
        User user = usermapper.selectUserById(userId);
        List<Contribution> contributions = contributionmapper.selectContributionsByAuthorId(userId);
        List<R_OverviewContribution> rOverviewContributions = new ArrayList<>();
        for(Contribution contribution : contributions) {
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(), user.getUsername());
            rOverviewContributions.add(rOverviewContribution);
        }
        return rOverviewContributions;
    }

    @Override
    public R_Audit_My_ContributionsDTO getMyContributions(String userId) {
        User user = usermapper.selectUserById(userId);
        List<R_OverviewContribution> pendingContributions = new ArrayList<>();
        List<R_OverviewContribution> approvedContributions = new ArrayList<>();
        List<R_OverviewContribution> dismissalContributions = new ArrayList<>();
        List<Contribution> pendingList = contributionmapper.selectContributionsByAuthorIdAndAuditStatus(userId,pending);
        List<Contribution> approvedList = contributionmapper.selectContributionsByAuthorIdAndAuditStatus(userId,approved);
        List<Contribution> dismissalList = contributionmapper.selectContributionsByAuthorIdAndAuditStatus(userId, dismissal);
        for(Contribution contribution : pendingList) {
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(), user.getUsername());
            pendingContributions.add(rOverviewContribution);
        }
        for(Contribution contribution : approvedList) {
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(), user.getUsername());
            approvedContributions.add(rOverviewContribution);
        }
        for(Contribution contribution : dismissalList) {
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(), user.getUsername());
            dismissalContributions.add(rOverviewContribution);
        }
        R_Audit_My_ContributionsDTO myContributionsDTO = new R_Audit_My_ContributionsDTO();
        myContributionsDTO.setPendingContributions(pendingContributions);
        myContributionsDTO.setApprovedContributions(approvedContributions);
        myContributionsDTO.setDismissalContributions(dismissalContributions);
        return myContributionsDTO;
    }

    @Override
    public List<R_User> getConcernedList(String userId) {
        List<User> concernedUsers = usermapper.selectFollowedUsersByUserId(userId);
        List<R_User> rUsers = new ArrayList<>();
        for(User user : concernedUsers) {
            R_User rUser = transformService.transformUserToRUser(user);
            rUsers.add(rUser);
        }
        return rUsers;
    }

    @Override
    public List<R_OverviewContribution> getLikedList(String userId) {
        List<Contribution> likedContributions = contributionmapper.selectLikeContributionsByUserId(userId);
        List<R_OverviewContribution> rOverviewContributions = new ArrayList<>();
        for(Contribution contribution : likedContributions) {
            User user = usermapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(), user.getUsername());
            rOverviewContributions.add(rOverviewContribution);
        }
        return rOverviewContributions;
    }

    @Override
    public List<R_OverviewContribution> getFavouriteList(String userId) {
        List<Contribution> favouriteContributions = contributionmapper.selectFavoriteContributionsByUserId(userId);
        List<R_OverviewContribution> rOverviewContributions = new ArrayList<>();
        for(Contribution contribution : favouriteContributions) {
            User user = usermapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(), user.getUsername());
            rOverviewContributions.add(rOverviewContribution);
        }
        return rOverviewContributions;
    }

    @Override
    public List<R_UserComment> getUserCommentList(String userId) {
        List<R_UserComment> rUserComments = new ArrayList<>();
        List<Comment> comments = commentmapper.selectCommentsByAuthorId(userId);
        for(Comment comment : comments) {
            Contribution contribution = contributionmapper.selectContributionById(comment.getContribution());
            if(contribution == null) {
                continue; // 作品不存在，跳过该评论
            }
            User contributionUser = usermapper.selectUserById(contribution.getAuthorId());
            User commentUser = usermapper.selectUserById(comment.getAuthor());
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername());
            R_UserComment rUserComment = transformService.transformCommentToRUserComment(comment, rOverviewContribution,commentUser.getAvatar(), commentUser.getUsername());
            rUserComments.add(rUserComment);
        }
        return rUserComments;
    }

    @Override
    public boolean deleteComment(String commentId, String userId) {
        Comment comment = commentmapper.selectCommentById(commentId);
        if(comment == null || !comment.getAuthor().equals(userId)) {
            return false; // 评论不存在或用户无权限删除
        }
        commentmapper.deleteCommentById(commentId);
        Contribution contribution = contributionmapper.selectContributionById(comment.getContribution());
        if(contribution != null) {
            contribution.setCommentCount(Math.max(0,contribution.getCommentCount() - 1));
            contributionmapper.updateContribution(contribution);
        }
        return true;
    }


    @Override
    public boolean deleteContribution(String contributionId, String userId) {
        Contribution contribution = contributionmapper.selectContributionById(contributionId);
        if(contribution == null || !contribution.getAuthorId().equals(userId)) {
            return false; // 作品不存在或用户无权限删除
        }
        contributionmapper.deleteContributionById(contributionId);
        return true;
    }

    @Override
    public boolean deletePendingContribution(String contributionId, String userId) {
        Contribution contribution = contributionmapper.selectNoAuditContributionById(contributionId);
        if(contribution == null || !contribution.getAuthorId().equals(userId)) {
            return false; // 待审核作品不存在或用户无权限删除
        }
        contributionmapper.deleteContributionById(contributionId);
        return true;
    }

    @Override
    public boolean deleteDismissalContribution(String contributionId, String userId) {
        Contribution contribution = contributionmapper.selectDismissalContributionById(contributionId);
        if(contribution == null || !contribution.getAuthorId().equals(userId)) {
            return false; // 驳回作品不存在或用户无权限删除
        }
        contributionmapper.deleteContributionById(contributionId);
        return true;
    }

    @Override
    public R_UserInfoDTO getUserInfo(String requesterId, String userId) {
        User user = usermapper.selectUserById(userId);
        if(user == null) {
            return null; // 用户不存在
        }
        R_User rUser = transformService.transformUserToRUser(user);
        R_UserInfoDTO userInfoDTO = new R_UserInfoDTO();
        userInfoDTO.setUser(rUser);
        boolean isConcerned = followmapper.isFollow(requesterId, userId);
        userInfoDTO.setIsConcerned(isConcerned);
        return userInfoDTO;
    }

    @Override
    public boolean updateUserInfo(String userId, String newUsername, Integer newGender, MultipartFile newAvatar) {
        try {// 1) 查询用户是否存在
            User user = usermapper.selectUserById(userId);
            if (user == null) {
                return false; // 用户不存在
            }
            // 2) 处理用户名变更：非空则校验唯一性
            if (newUsername != null && !newUsername.isEmpty()) {
                User existingUser = usermapper.selectUserByName(newUsername);
                if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                    return false; // 新用户名已被其他用户使用
                }
                user.setUsername(newUsername);
            }
            // 3) 处理性别变更：入参不为空则覆盖
            if (newGender != null) {
                user.setSex(newGender);
            }
            // 4) 处理头像变更：仅当上传了新头像时才保存并覆盖为新 URL
            if (newAvatar != null && !newAvatar.isEmpty()) {
                String avatarUrl = fileStorageService.saveAvatar(newAvatar, userId);
                user.setAvatar(avatarUrl);
            }
            // 5) 落库更新
            usermapper.updateUser(user);
            return true;
        }catch(Exception e){
            throw new RuntimeException("仅支持图片类型文件", e);
        }
    }

    @Override
    public boolean concernUser(String userId, String concernedUserId) {
        User user = usermapper.selectUserById(userId);
        User concernedUser = usermapper.selectUserById(concernedUserId);
        if(user == null || concernedUser == null) {
            return false; // 用户不存在
        }
        if(followmapper.isFollow(userId, concernedUserId)) {
            return true; // 已关注，幂等处理
        }
        followmapper.insertFollow(userId, concernedUserId);
        return true;
    }

    @Override
    public boolean unconcernUser(String userId, String concernedUserId) {
        User user = usermapper.selectUserById(userId);
        User concernedUser = usermapper.selectUserById(concernedUserId);
        if (user == null || concernedUser == null) {
            return false; // 用户不存在
        }
        if (!followmapper.isFollow(userId, concernedUserId)) {
            return true; // 未关注，幂等处理
        }
        followmapper.deleteFollow(userId, concernedUserId);
        return true;
    }

    @Override
    public List<String> getMySecurityIssues(String username) {
        User user = usermapper.selectUserByName(username);
        if (user == null) {
            return null; // 用户不存在
        }
        List<SecurityIssue> securityIssues = securityissuemapper.selectSecurityIssuesByUserId(user.getUserId());
        List<String> questions = new ArrayList<>();
        for (SecurityIssue issue : securityIssues) {
            questions.add(issue.getDescription());
        }
        return questions;
    }

    @Override
    public R_VerifySecurityIssuesDTO verifySecurityIssues(String username, List<R_SecurityIssue> securityIssues) {
        User user = usermapper.selectUserByName(username);
        if (user == null) {
            return null; // 用户不存在
        }
        List<SecurityIssue> storedIssues = securityissuemapper.selectSecurityIssuesByUserId(user.getUserId());
        if (storedIssues.size() != securityIssues.size()) {
            return null; // 问题数量不匹配
        }
        for (R_SecurityIssue rIssue : securityIssues) {
            boolean matchFound = false;
            for (SecurityIssue storedIssue : storedIssues) {
                if (storedIssue.getDescription().equals(rIssue.getDescription()) &&
                        storedIssue.getAnswer().equals(rIssue.getAnswer())) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return null; // 有问题不匹配
            }
        }
        String token = JwtUtil.genToken(new Claims(username, user.getUserId(), user.getRole(), updatePWD).toMap());
        R_VerifySecurityIssuesDTO dto = new R_VerifySecurityIssuesDTO();
        dto.setVerified(true);
        dto.setTempToken(token);
        return dto ;
    }
}
