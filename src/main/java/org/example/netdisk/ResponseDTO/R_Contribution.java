package org.example.netdisk.ResponseDTO;

import lombok.Data;
import org.example.PCOI.Entity.Tag;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class R_Contribution {
    private String contributionId;              // 作品ID
    private Integer type;            // 作品类型
    private String title;         // 作品标题
    private List<String> image;         // 作品图片路径
    private String description;  // 描述信息
    private Integer status;        // 状态(封禁状态，正常状态）
    private Integer auditStatus;   // 审核状态（待审核，已通过，已驳回）
    private LocalDateTime publishTime;         // 上传时间
    private String authorId;     // 上传者
    private String authorName;   // 上传者名称
    private String uploaderAvatarPath; // 上传者头像路径
    private Integer viewCount;      // 浏览数
    private Integer favoriteCount;        // 收藏数
    private Integer likeCount;      // 点赞数
    private Integer commentCount;   // 评论数
    private String dismissalReason; // 驳回理由
    private List<Tag> tags;        // 作品标签
}
