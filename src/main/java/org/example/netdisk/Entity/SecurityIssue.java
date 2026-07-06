package org.example.netdisk.Entity;

import lombok.Data;

@Data
public class SecurityIssue {
    private String userId;          // 用户ID
    private String description;       // 密保问题
    private String answer;        // 密保答案
}
