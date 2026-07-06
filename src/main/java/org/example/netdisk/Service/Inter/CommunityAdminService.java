package org.example.netdisk.Service.Inter;

import org.example.PCOI.ResponseDTO.R_Audit_My_ContributionsDTO;
import org.example.PCOI.ResponseDTO.R_Contribution;
import org.example.PCOI.ResponseDTO.R_OverviewContribution;
import org.example.PCOI.ResponseDTO.R_User;

import java.util.List;


public interface CommunityAdminService {
    boolean blockUser(String userId);
    boolean unblockUser(String userId);
    boolean blockContribution(String contributionId);
    R_Contribution getBannedContribution(String contributionId);
    boolean unblockContribution(String contributionId);
    R_Audit_My_ContributionsDTO auditContributions();
    boolean dismissContribution(String contributionId, String dismissalReason);
    boolean approveContribution(String contributionId);
    List<R_User> getBlockedUsers();
    List<R_OverviewContribution> getBlockedContributions();
    boolean deleteComment(String commentId);
}

