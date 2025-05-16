package com.christabella.africahr.leavemanagement.dto;

import lombok.Data;

@Data
public class BalanceAdjustmentRequest {
    private String userId;
    private Long leaveTypeId;
    private Double newBalance;
    private Double usedDays;
    private String reason;
    private Double carryOver;
}
