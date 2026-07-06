package org.example.netdisk.Service.Support;

import org.example.PCOI.Entity.*;
import org.example.PCOI.ResponseDTO.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransformService {
    public R_User transformUserToRUser(User user)
    {
        R_User rUser = new R_User();
        rUser.setUserId(user.getUserId());
        rUser.setRole(user.getRole());
        rUser.setStatus(user.getStatus());
        rUser.setUsername(user.getUsername());
        rUser.setSex(user.getSex());
        rUser.setAvatar(user.getAvatar());
        return rUser;
    }

    public SecurityIssue transformRSecurityIssueToSecurityIssue(R_SecurityIssue rSecurityIssue,String userId)
    {
        SecurityIssue securityIssue = new SecurityIssue();
        securityIssue.setUserId(userId);
        securityIssue.setDescription(rSecurityIssue.getDescription());
        securityIssue.setAnswer(rSecurityIssue.getAnswer());
        return securityIssue;
    }

    public R_OverviewContribution transformContributionToROverviewContribution(Contribution contribution,String avatar,String authorName)
    {
        R_OverviewContribution rOverviewContribution = new R_OverviewContribution();
        rOverviewContribution.setContributionId(contribution.getContributionId());
        rOverviewContribution.setTitle(contribution.getTitle());
        rOverviewContribution.setAuthorId(contribution.getAuthorId());
        rOverviewContribution.setImage(contribution.getImage());
        rOverviewContribution.setViewCount(contribution.getViewCount());
        rOverviewContribution.setFavoriteCount(contribution.getFavoriteCount());
        rOverviewContribution.setLikeCount(contribution.getLikeCount());
        rOverviewContribution.setCommentCount(contribution.getCommentCount());
        rOverviewContribution.setDismissalReason(contribution.getDismissalReason());
        rOverviewContribution.setAvatar(avatar);
        rOverviewContribution.setAuthorName(authorName);
        return rOverviewContribution;
    }

    public R_ContributionComment transformCommentToRContributionComment(Comment comment,String avatar,String authorName)
    {
        R_ContributionComment rContributionComment = new R_ContributionComment();
        rContributionComment.setCommentId(comment.getCommentId());
        rContributionComment.setAuthor(comment.getAuthor());
        rContributionComment.setDescription(comment.getDescription());
        rContributionComment.setAvatar(avatar);
        rContributionComment.setTime(comment.getTime());
        rContributionComment.setAuthorName(authorName);
        return rContributionComment;
    }

    public R_UserComment transformCommentToRUserComment(Comment comment, R_OverviewContribution contribution,String avatar,String authorName)
    {
        R_UserComment rUserComment = new R_UserComment();
        rUserComment.setComment(transformCommentToRContributionComment(comment,avatar,authorName));
        rUserComment.setContribution(contribution);
        return rUserComment;
    }

    public R_Contribution transformContributionToRContribution(Contribution contribution, String avatar, String authorName, List<Tag> tags)
    {
        R_Contribution rContribution = new R_Contribution();
        rContribution.setType(contribution.getType());
        rContribution.setContributionId(contribution.getContributionId());
        rContribution.setTitle(contribution.getTitle());
        rContribution.setAuthorId(contribution.getAuthorId());
        rContribution.setDescription(contribution.getDescription());
        rContribution.setImage(contribution.getImage());
        rContribution.setViewCount(contribution.getViewCount());
        rContribution.setFavoriteCount(contribution.getFavoriteCount());
        rContribution.setLikeCount(contribution.getLikeCount());
        rContribution.setCommentCount(contribution.getCommentCount());
        rContribution.setStatus(contribution.getStatus());
        rContribution.setDismissalReason(contribution.getDismissalReason());
        rContribution.setPublishTime(contribution.getPublishTime());
        rContribution.setAuditStatus(contribution.getAuditStatus());
        rContribution.setUploaderAvatarPath(avatar);
        rContribution.setAuthorName(authorName);
        rContribution.setTags(tags);
        return rContribution;
    }
}
