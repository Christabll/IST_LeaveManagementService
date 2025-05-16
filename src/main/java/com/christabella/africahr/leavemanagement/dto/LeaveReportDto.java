package com.christabella.africahr.leavemanagement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaveReportDto {
    private String employeeName;
    private String employeeEmail;
    private String department;
    private String role;
    private String leaveType;
    private String status;
    private String startDate;
    private String endDate;
    private long days;
    private String reason;
    private String documentUrl;
    private String approverName;
    private String approverComment;
} 