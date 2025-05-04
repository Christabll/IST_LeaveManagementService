package com.christabella.africahr.leavemanagement.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDto {
    private String employeeName;
    private String leaveType;
    private long totalDaysTaken;
}
