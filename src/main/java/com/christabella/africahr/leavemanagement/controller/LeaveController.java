package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.dto.*;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.repository.LeaveBalanceRepository;
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

import java.util.List;
import java.util.stream.Collectors;

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
    public ResponseEntity<ApiResponse<LeaveRequest>> applyLeave(@RequestBody LeaveRequestDto dto) {
        LeaveRequest request = leaveService.applyForLeave(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<LeaveRequest>builder()
                        .success(true)
                        .message("Leave request submitted successfully")
                        .data(request)
                        .build()
        );
    }

    //  Get My Requests
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

    //For Managers/Admins to view any user's leave balance
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

    //For currently logged-in user to check their own leave balance
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


    //  View Balance
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


    //  Public Holidays
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

    //  Team Members on Leave
    @GetMapping("/on-leave")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> teamOnLeave() {
        ApiResponse<List<LeaveRequest>> response = leaveService.getCurrentEmployeesOnLeave();
        return ResponseEntity.ok(response);
    }


}
