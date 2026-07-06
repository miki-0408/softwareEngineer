package org.example.netdisk.ResponseDTO;

import lombok.Data;

import java.util.List;

@Data
public class R_ContributionDTO {
    private R_Contribution contribution; // 作品信息
    private List<R_ContributionComment> comments; // 作品评论列表
    private Boolean isLiked;          // 是否已点赞
    private Boolean isFavorite;     // 是否已收藏
}
