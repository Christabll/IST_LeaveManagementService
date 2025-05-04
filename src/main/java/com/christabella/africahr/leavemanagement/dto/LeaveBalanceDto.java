package com.christabella.africahr.leavemanagement.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LeaveBalanceDto {
    private String leaveType;
    private double defaultBalance;
    private double balance;
    private double carryOver;
    private int usedLeave;
}
