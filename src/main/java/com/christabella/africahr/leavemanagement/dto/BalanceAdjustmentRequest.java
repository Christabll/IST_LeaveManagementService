package com.christabella.africahr.leavemanagement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BalanceAdjustmentRequest {
    private String userId;
    private Long leaveTypeId;
    private double newBalance;
}
