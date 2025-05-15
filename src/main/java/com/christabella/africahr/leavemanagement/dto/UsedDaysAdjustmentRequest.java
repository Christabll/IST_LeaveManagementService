package com.christabella.africahr.leavemanagement.dto;

import lombok.Data;

@Data
public class UsedDaysAdjustmentRequest {
    private String userId;
    private Long leaveTypeId;
    private Double usedDays;
    private String reason;
} 