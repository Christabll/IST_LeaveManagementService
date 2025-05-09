package com.christabella.africahr.leavemanagement.dto;

import lombok.*;

@Data
@Builder
public class LeaveTypeDto {
    private String name;
    private double defaultBalance;
    private boolean requiresDocument;

}

