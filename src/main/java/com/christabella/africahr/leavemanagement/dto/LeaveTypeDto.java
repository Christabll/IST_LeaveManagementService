package com.christabella.africahr.leavemanagement.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LeaveTypeDto {
    private Long id;
    private String name;
    private double defaultBalance;
}
