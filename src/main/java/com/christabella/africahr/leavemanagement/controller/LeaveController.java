package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.dto.*;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.exception.BadRequestException;
import com.christabella.africahr.leavemanagement.security.CustomUserDetails;
import com.christabella.africahr.leavemanagement.service.LeaveBalanceService;
import com.christabella.africahr.leavemanagement.service.LeaveService;
import com.christabella.africahr.leavemanagement.service.LeaveTypeService;
import com.christabella.africahr.leavemanagement.service.PublicHolidayService;
import com.christabella.africahr.leavemanagement.service.UserServiceClient;

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
    private final UserServiceClient userServiceClient; 

    

    @PreAuthorize("hasAuthority('STAFF')")
    @PostMapping("/apply")
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

    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    @GetMapping("/my-requests")
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

    @PreAuthorize("hasAnyAuthority('MANAGER', 'STAFF', 'ADMIN')")
    @GetMapping("/leave-types")
    public ResponseEntity<ApiResponse<List<LeaveTypeDto>>> getAllLeaveTypes() {
        List<LeaveTypeDto> types = leaveTypeService.getAllLeaveTypes();
        return ResponseEntity.ok(ApiResponse.<List<LeaveTypeDto>>builder()
                .success(true)
                .message("Fetched leave types")
                .data(types)
                .build());
    }

    @PreAuthorize("hasAnyAuthority('STAFF', 'MANAGER', 'ADMIN')")
    @GetMapping("/balance") 
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> viewMyBalance(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String userId = userDetails.getId();
    
        if (userId.contains("@")) {
            userId = userServiceClient.getUserIdByEmail(userId);
            if (userId == null || userId.contains("@")) {
                throw new BadRequestException("Unable to resolve user UUID");
            }
        }
        
        List<LeaveBalanceDto> balances = leaveBalanceService.getBalancesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.<List<LeaveBalanceDto>>builder()
                .success(true)
                .message("My leave balance")
                .data(balances)
                .build());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    @GetMapping("/public-holidays")
    public ResponseEntity<ApiResponse<List<PublicHolidayDto>>> getHolidays() {
        List<PublicHolidayDto> holidays = publicHolidayService.getUpcomingHolidays();
        return ResponseEntity.ok(ApiResponse.<List<PublicHolidayDto>>builder()
                .success(true)
                .message("Upcoming public holidays")
                .data(holidays)
                .build());
    }

    @PreAuthorize("hasAnyAuthority('STAFF','MANAGER','ADMIN')")
    @GetMapping("/on-leave")
    public ResponseEntity<ApiResponse<List<TeamOnLeaveDto>>> teamOnLeave(@RequestParam(required = false) String department) {
        return ResponseEntity.ok(leaveService.getTeamOnLeaveTooltipInfo(department));
    }
}