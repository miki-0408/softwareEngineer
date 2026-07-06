package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.PCOI.Entity.Comment;
import org.example.PCOI.Entity.Contribution;
import org.example.PCOI.Entity.Tag;
import org.example.PCOI.Entity.User;
import org.example.PCOI.Mapper.*;
import org.example.PCOI.ResponseDTO.R_Audit_My_ContributionsDTO;
import org.example.PCOI.ResponseDTO.R_Contribution;
import org.example.PCOI.ResponseDTO.R_OverviewContribution;
import org.example.PCOI.ResponseDTO.R_User;
import org.example.PCOI.Service.Inter.CommunityAdminService;
import org.example.PCOI.Service.Support.TransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.example.PCOI.Service.Support.Enum.*;

@Slf4j
@Service
public class CommunityAdminServiceImpl implements CommunityAdminService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ContributionMapper contributionMapper;
    @Autowired
    private TransformService transformService;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private TagRelationMapper tagRelationMapper;
    @Autowired
    private TagMapper tagMapper;

    @Override
    public boolean blockUser(String userId) {
        // 查询用户是否存在
        User user = userMapper.selectUserById(userId);
        if(user==null)
            return false;
        // 设置用户状态为封禁
        user.setStatus(banned);
        userMapper.updateUser(user);
        return true;
    }

    @Override
    public boolean unblockUser(String userId) {
        // 查询用户是否存在
        User user = userMapper.selectUserById(userId);
        if(user==null)
            return false;
        // 恢复用户为正常状态
        user.setStatus(normal);
        userMapper.updateUser(user);
        return true;
    }

    @Override
    public boolean blockContribution(String contributionId) {
        // 查询作品是否存在
        Contribution contribution = contributionMapper.selectContributionById(contributionId);
        if(contribution==null||contribution.getAuditStatus()!=approved)
            return false;
        // 设置作品状态为封禁
        contribution.setStatus(banned);
        contributionMapper.updateContribution(contribution);
        return true;
    }

    @Override
    public boolean unblockContribution(String contributionId) {
        // 查询作品是否存在
        Contribution contribution = contributionMapper.selectBannedContributionById(contributionId);
        if(contribution==null||contribution.getAuditStatus()!=approved)
            return false;
        contribution.setStatus(normal);
        contributionMapper.updateContribution(contribution);
        return true;
    }

    @Override
    public R_Audit_My_ContributionsDTO auditContributions() {
        // 为三种审核状态分别准备承载概览结果的列表
        List<R_OverviewContribution> pendingContributions = new ArrayList<>();
        List<R_OverviewContribution> approvedContributions = new ArrayList<>();
        List<R_OverviewContribution> dismissalContributions = new ArrayList<>();

        // 按审核状态查询原始作品列表
        List<Contribution> pendingList = contributionMapper.selectContributionsByAuditStatus(pending);
        List<Contribution> approvedList = contributionMapper.selectContributionsByAuditStatus(approved);
        List<Contribution> dismissalList = contributionMapper.selectContributionsByAuditStatus(dismissal);

        // 组装待审核作品概览：补充作者头像并做模型转换
        for(Contribution contribution : pendingList){
            User user = userMapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution =
                    transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(),user.getUsername());
            pendingContributions.add(rOverviewContribution);
        }
        // 组装已通过作品概览
        for(Contribution contribution : approvedList){
            User user = userMapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution =
                    transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(),user.getUsername());
            approvedContributions.add(rOverviewContribution);
        }
        // 组装已驳回作品概览
        for(Contribution contribution : dismissalList){
            User user = userMapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution =
                    transformService.transformContributionToROverviewContribution(contribution, user.getAvatar(),user.getUsername());
            dismissalContributions.add(rOverviewContribution);
        }

        // 汇总返回 DTO
        R_Audit_My_ContributionsDTO rAuditContributionsDTO = new R_Audit_My_ContributionsDTO();
        rAuditContributionsDTO.setPendingContributions(pendingContributions);
        rAuditContributionsDTO.setApprovedContributions(approvedContributions);
        rAuditContributionsDTO.setDismissalContributions(dismissalContributions);
        return rAuditContributionsDTO;
    }

    @Override
    public boolean dismissContribution(String contributionId, String dismissalReason) {
        // 查询目标作品是否存在
        Contribution contribution = contributionMapper.selectNoAuditContributionById(contributionId);
        if (contribution == null)
            return false;
        // 更新状态与驳回原因并持久化
        contribution.setAuditStatus(dismissal);
        contribution.setDismissalReason(dismissalReason);
        contributionMapper.updateContribution(contribution);
        return true;
    }

    @Override
    public boolean approveContribution(String contributionId) {
        // 查询目标作品是否存在
        Contribution contribution = contributionMapper.selectNoAuditContributionById(contributionId);
        if (contribution == null)
            return false;
        // 更新状态为通过并清空驳回原因，随后持久化
        contribution.setAuditStatus(approved);
        contribution.setDismissalReason(null);
        contributionMapper.updateContribution(contribution);
        return true;
    }

    @Override
    public List<R_User> getBlockedUsers() {
        List<R_User> blockedRUsers = new ArrayList<>();
        List<User> blockedUsers = userMapper.selectUsersByStatus(banned);
        for(User user : blockedUsers){
            R_User rUser = transformService.transformUserToRUser(user);
            blockedRUsers.add(rUser);
        }
        return blockedRUsers;
    }

    @Override
    public List<R_OverviewContribution> getBlockedContributions() {
        List<R_OverviewContribution> blockedRContributions = new ArrayList<>();
        List<Contribution> blockedContributions = contributionMapper.selectContributionsByStatus(banned);
        for(Contribution contribution : blockedContributions){
            User user = userMapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution,user.getAvatar(),user.getUsername());
            blockedRContributions.add(rOverviewContribution);
        }
        return blockedRContributions;
    }

    @Override
    public R_Contribution getBannedContribution(String contributionId) {
        Contribution contribution = contributionMapper.selectBannedContributionById(contributionId);
        if(contribution==null||contribution.getAuditStatus()!=approved||contribution.getStatus()!=banned)
            return null;
        User user = userMapper.selectUserById(contribution.getAuthorId());
        List<Integer> tagIds = tagRelationMapper.getContributionTags(contributionId);
        List<Tag> tags = new ArrayList<>();
        for(Integer tagId : tagIds){
            Tag tag = tagMapper.selectTagById(tagId);
            tags.add(tag);
        }
        return transformService.transformContributionToRContribution(contribution,user.getAvatar(),user.getUsername(),tags);
    }

    @Override
    public boolean deleteComment(String commentId) {
        Comment comment = commentMapper.selectCommentById(commentId);
        if(comment==null)
            return false;
        commentMapper.deleteCommentById(commentId);
        Contribution contribution = contributionMapper.selectContributionById(comment.getContribution());
        if(contribution!=null){
            contribution.setCommentCount(Math.max(0,contribution.getCommentCount() - 1));
            contributionMapper.updateContribution(contribution);
        }
        return true;
    }
}
