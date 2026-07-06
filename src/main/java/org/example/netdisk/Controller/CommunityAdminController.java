package org.example.netdisk.Controller;

import org.example.PCOI.ResponseDTO.*;
import org.example.PCOI.Service.Inter.CommunityAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class CommunityAdminController {
    @Autowired
    private CommunityAdminService communityAdminService;

    @PostMapping("/communityAdmin/blockUser")
    public Result<String> blockUser(
            @RequestParam("userId") String userId) {
        boolean ok = communityAdminService.blockUser(userId);
        if (ok) {
            return Result.success("封禁用户成功");
        }
        return Result.error("封禁用户失败");
    }

    @PostMapping("/communityAdmin/unblockUser")
    public Result<String> unblockUser(
            @RequestParam("userId") String userId) {
        boolean ok = communityAdminService.unblockUser(userId);
        if (ok) {
            return Result.success("解封用户成功");
        }
        return Result.error("解封用户失败");
    }

    @PostMapping("/communityAdmin/blockContribution")
    public Result<String> blockContribution(
            @RequestParam("contributionId") String contributionId) {
        boolean ok = communityAdminService.blockContribution(contributionId);
        if (ok) {
            return Result.success("封禁作品成功");
        }
        return Result.error("封禁作品失败");
    }

    @PostMapping("/communityAdmin/bannedContribution")
    public Result<R_Contribution> bannedContribution(
            @RequestParam("contributionId") String contributionId) {
        R_Contribution c = communityAdminService.getBannedContribution(contributionId);
        return Result.success(c);
    }

    @PostMapping("/communityAdmin/unblockContribution")
    public Result<String> unblockContribution(
            @RequestParam("contributionId") String contributionId) {
        boolean ok = communityAdminService.unblockContribution(contributionId);
        if (ok) {
            return Result.success("解封作品成功");
        }
        return Result.error("解封作品失败");
    }

    @GetMapping("/communityAdmin/auditContributions")
    public Result<R_Audit_My_ContributionsDTO> auditContributions() {
        R_Audit_My_ContributionsDTO data = communityAdminService.auditContributions();
        return Result.success(data);
    }

    @PostMapping("/communityAdmin/dismissContribution")
    public Result<String> dismissContribution(
            @RequestParam("contributionId") String contributionId,
            @RequestParam("dismissalReason") String dismissalReason) {
        boolean ok = communityAdminService.dismissContribution(contributionId, dismissalReason);
        if (ok) {
            return Result.success("已驳回作品");
        }
        return Result.error("驳回作品失败");
    }

    @PostMapping("/communityAdmin/approveContribution")
    public Result<String> approveContribution(
            @RequestParam("contributionId") String contributionId) {
        boolean ok = communityAdminService.approveContribution(contributionId);
        if (ok) {
            return Result.success("已通过审核");
        }
        return Result.error("通过审核失败");
    }

    @GetMapping("/communityAdmin/blockedUsers")
    public Result<List<R_User>> getBlockedUsers() {
        List<R_User> list = communityAdminService.getBlockedUsers();
        return Result.success(list);
    }

    @GetMapping("/communityAdmin/blockedContributions")
    public Result<List<R_OverviewContribution>> getBlockedContributions() {
        List<R_OverviewContribution> list = communityAdminService.getBlockedContributions();
        return Result.success(list);
    }

    @PostMapping("/communityAdmin/deleteComment")
    public Result<String> deleteComment(
            @RequestParam("commentId") String commentId) {
        boolean ok = communityAdminService.deleteComment(commentId);
        if (ok) {
            return Result.success("删除评论成功");
        }
        return Result.error("删除评论失败");
    }

}
