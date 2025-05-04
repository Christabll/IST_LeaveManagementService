package com.christabella.africahr.leavemanagement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class PublicHolidayDto {
    private LocalDate date;
    private String name;
}

