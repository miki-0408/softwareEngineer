package org.example.netdisk.ResponseDTO;

import lombok.Data;

import java.util.List;

@Data
public class R_Audit_My_ContributionsDTO {
    private List<R_OverviewContribution> pendingContributions; // 待审核作品列表
    private List<R_OverviewContribution> approvedContributions; // 审核通过作品列表
    private List<R_OverviewContribution> dismissalContributions; // 审核未通过作品列表
}
