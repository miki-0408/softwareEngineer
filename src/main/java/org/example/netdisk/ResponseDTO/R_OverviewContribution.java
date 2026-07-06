package org.example.netdisk.ResponseDTO;

import lombok.Data;

import java.util.List;

@Data
public class R_OverviewContribution {
    private String contributionId; // 作品ID
    private String title;          // 作品标题
    private String authorId;       // 上传者
    private String authorName;     // 上传者名称
    private List<String> image;      // 作品图片路径（多图）
    private Integer viewCount;      // 浏览数
    private Integer favoriteCount;        // 收藏数
    private Integer likeCount;      // 点赞数
    private Integer commentCount;   // 评论数
    private String dismissalReason;   // 驳回理由
    private String avatar; // 上传者头像路径
}
