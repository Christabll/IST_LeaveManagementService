package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.dto.PublicHolidayDto;
import com.christabella.africahr.leavemanagement.service.ExternalHolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class PublicHolidayController {
    private final ExternalHolidayService externalHolidayService;

    @GetMapping("/rwanda/{year}")
    public ResponseEntity<List<PublicHolidayDto>> getRwandaPublicHolidays(@PathVariable int year) {
        return ResponseEntity.ok(externalHolidayService.fetchRwandaPublicHolidays(year));
    }
}