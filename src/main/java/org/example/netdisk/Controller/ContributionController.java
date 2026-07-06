package org.example.netdisk.Controller;
import org.example.PCOI.ResponseDTO.*;
import org.example.PCOI.Service.Inter.ContributionService;
import org.example.PCOI.Utils.TokenProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
public class ContributionController {
    @Autowired
    private ContributionService contributionService;

    @GetMapping("/illustrations")
    public Result<List<R_OverviewContribution>> getIllustrations() {
        List<R_OverviewContribution> list = contributionService.getIllustrations();
        return Result.success(list);
    }

    @GetMapping("/mangas")
    public Result<List<R_OverviewContribution>> getMangas() {
        List<R_OverviewContribution> list = contributionService.getMangas();
        return Result.success(list);
    }

    @GetMapping("/allContributions")
    public Result<List<R_OverviewContribution>> getAllContributions() {
        List<R_OverviewContribution> list = contributionService.getAllContributions();
        return Result.success(list);
    }

    @PostMapping("/contribution")
    public Result<R_ContributionDTO> getContribution(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("contributionId") String contributionId) throws Exception {
        String userId = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        R_ContributionDTO data = contributionService.getContribution(userId,contributionId);
        return Result.success(data);
    }

    @PostMapping("/pendingContribution")
    public Result<R_Contribution> getPendingContribution(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("contributionId") String contributionId) throws Exception {
        String userId = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        Integer role = (Integer) TokenProcess.getAttributeFromToken(authHeader, "role");
        R_Contribution c = contributionService.getPendingContribution(userId,role,contributionId);
        return Result.success(c);
    }

    @PostMapping("/contributionsRanking")
    public Result<List<R_OverviewContribution>> getContributionsRanking(
            @RequestParam("type") Integer type,
            @RequestParam("key") Integer key) {
        List<R_OverviewContribution> list = contributionService.getContributionsRanking(type, key);
        return Result.success(list);
    }

    @PostMapping("/user/likeContribution")
    public Result<String> likeContribution(
            @RequestHeader ("Authorization") String authHeader,
            @RequestParam("contributionId") String contributionId) throws Exception {
        String userId = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        boolean ok = contributionService.likeContribution(userId, contributionId);
        if (ok) {
            return Result.success("点赞成功");
        }
        return Result.error("点赞失败");
    }

    @PostMapping("/user/unlikeContribution")
    public Result<String> unlikeContribution(
            @RequestHeader ("Authorization") String authHeader,
            @RequestParam("contributionId") String contributionId) throws Exception {
        String userId = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        boolean ok = contributionService.unlikeContribution(userId, contributionId);
        if (ok) {
            return Result.success("取消点赞成功");
        }
        return Result.error("取消点赞失败");
    }

    @PostMapping("/user/favoriteContribution")
    public Result<String> favoriteContribution(
            @RequestHeader ("Authorization") String authHeader,
            @RequestParam("contributionId") String contributionId) throws Exception {
        String userId = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        boolean ok = contributionService.favoriteContribution(userId, contributionId);
        if (ok) {
            return Result.success("收藏成功");
        }
        return Result.error("收藏失败");
    }

    @PostMapping("/user/unfavoriteContribution")
    public Result<String> unfavoriteContribution(
            @RequestHeader ("Authorization") String authHeader,
            @RequestParam("contributionId") String contributionId) throws Exception {
        String userId = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        boolean ok = contributionService.unfavoriteContribution(userId, contributionId);
        if (ok) {
            return Result.success("取消收藏成功");
        }
        return Result.error("取消收藏失败");
    }

    @PostMapping("/user/commentContribution")
    public Result<String> commentContribution(
            @RequestHeader ("Authorization") String authHeader,
            @RequestParam("contributionId") String contributionId,
            @RequestParam("comment") String comment) throws Exception {
        String userId = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
        boolean ok = contributionService.commentContribution(userId, contributionId, comment);
        if (ok) {
            return Result.success("评论成功");
        }
        return Result.error("评论失败");
    }

    @PostMapping("/user/uploadContribution")
    public Result<String> uploadContribution(
            @RequestHeader ("Authorization") String authHeader,
            @RequestParam("title") String title,
            @RequestParam("type") Integer type,
            @RequestParam("description") String description,
            @RequestPart(name = "tags", required = false) List<String> tags,
            @RequestPart(name = "images") List<MultipartFile> images) throws Exception {
        try {
            String userId = (String) TokenProcess.getAttributeFromToken(authHeader, "userId");
            boolean ok = contributionService.uploadContribution(userId, title, type, description, tags, images);
            if (ok) {
                return Result.success("上传成功");
            }
            return Result.error("上传失败");
        }catch (RuntimeException e){
            return Result.error("上传失败:"+e.getMessage());
        }
    }
}
