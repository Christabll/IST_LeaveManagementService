package com.christabella.africahr.leavemanagement.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LeaveRequestDto {
    private Long id;
    private String userId;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String documentUrl;
    private String email;
    private String role;
}
