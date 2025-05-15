package com.christabella.africahr.leavemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceDto {
    private String leaveType;
    private Long leaveTypeId;
    private double defaultBalance;
    private double usedLeave;
    private String remainingLeave;
    private double carryOver;
    private int year;
}



