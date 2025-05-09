package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.dto.*;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.security.CustomUserDetails;
import com.christabella.africahr.leavemanagement.service.LeaveBalanceService;
import com.christabella.africahr.leavemanagement.service.LeaveService;
import com.christabella.africahr.leavemanagement.service.LeaveTypeService;
import com.christabella.africahr.leavemanagement.service.PublicHolidayService;
import com.christabella.africahr.leavemanagement.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveTypeService leaveTypeService;
    private final PublicHolidayService publicHolidayService;



    @PostMapping("/apply")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<LeaveRequest>> applyLeave(
            @RequestPart("data") LeaveRequestDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        LeaveRequest request = leaveService.applyForLeave(dto, file);
        return ResponseEntity.status(201).body(
                ApiResponse.<LeaveRequest>builder()
                        .success(true)
                        .message("Leave request submitted successfully")
                        .data(request)
                        .build()
        );
    }


    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> myRequests() {
        List<LeaveRequest> requests = leaveService.getMyLeaveRequests();
        return ResponseEntity.ok(
                ApiResponse.<List<LeaveRequest>>builder()
                        .success(true)
                        .message("Fetched leave requests")
                        .data(requests)
                        .build()
        );
    }


    @GetMapping("/leave-types")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveTypeDto>>> getAllLeaveTypes() {
        List<LeaveTypeDto> types = leaveTypeService.getAllLeaveTypes();
        return ResponseEntity.ok(ApiResponse.<List<LeaveTypeDto>>builder()
                .success(true)
                .message("Fetched leave types")
                .data(types)
                .build());
    }


    @GetMapping("/leave/balance")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> viewMyBalance(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<LeaveBalanceDto> balances = leaveBalanceService.getBalancesByUserId(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.<List<LeaveBalanceDto>>builder()
                .success(true)
                .message("My leave balance")
                .data(balances)
                .build());
    }


    @GetMapping("/admin/leave/balance/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> viewBalanceForUser(@PathVariable String userId) {
        List<LeaveBalanceDto> balances = leaveBalanceService.getBalancesByUserId(userId);

        return ResponseEntity.ok(ApiResponse.<List<LeaveBalanceDto>>builder()
                .success(true)
                .message("User leave balance")
                .data(balances)
                .build());
    }


    @GetMapping("/public-holidays")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<PublicHolidayDto>>> getHolidays() {
        List<PublicHolidayDto> holidays = publicHolidayService.getUpcomingHolidays();
        return ResponseEntity.ok(ApiResponse.<List<PublicHolidayDto>>builder()
                .success(true)
                .message("Upcoming public holidays")
                .data(holidays)
                .build());
    }


    @GetMapping("/on-leave")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<TeamOnLeaveDto>>> teamOnLeave(@RequestParam(required = false) String department) {
        return ResponseEntity.ok(leaveService.getTeamOnLeaveTooltipInfo(department));
    }



}

