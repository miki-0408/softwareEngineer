package org.example.netdisk.ResponseDTO;

import lombok.Data;

@Data
public class R_UserComment {
    private R_ContributionComment comment; // 评论内容
    private R_OverviewContribution contribution; // 作品概览
}
