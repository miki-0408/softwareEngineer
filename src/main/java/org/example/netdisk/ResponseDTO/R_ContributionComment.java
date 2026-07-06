package org.example.netdisk.ResponseDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class R_ContributionComment {
    private String commentId; // 评论ID
    private String author;     // 评论用户
    private String authorName; // 评论用户名称
    private String description;      // 评论内容
    private LocalDateTime time;    // 评论时间戳
    private String avatar; // 用户头像路径
}
