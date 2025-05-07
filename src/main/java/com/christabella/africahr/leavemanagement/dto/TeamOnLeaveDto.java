package com.christabella.africahr.leavemanagement.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamOnLeaveDto {
    private String fullName;
    private String avatarUrl;
    private LocalDate leaveUntil;
}
