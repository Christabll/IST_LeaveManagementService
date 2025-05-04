package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.dto.*;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.entity.LeaveType;
import com.christabella.africahr.leavemanagement.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LeaveTypeService leaveTypeService;
    private final AdminService adminService;
    private final ReportingService reportingService;
    private final LeaveBalanceService leaveBalanceService;

    //  Create Leave Type
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/leave-types")
    public ResponseEntity<ApiResponse<LeaveType>> createLeaveType(@RequestBody LeaveTypeDto dto) {
        LeaveType type = leaveTypeService.createLeaveType(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<LeaveType>builder()
                        .success(true)
                        .message("Leave type created")
                        .data(type)
                        .build()
        );
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/leave-requests/{leaveRequestId}/approve")
    public ResponseEntity<ApiResponse<LeaveRequest>> approveRequest(
            @PathVariable("leaveRequestId") Long leaveRequestId,
            @RequestParam String comment
    ) {
        LeaveRequest request = adminService.approve(leaveRequestId, comment);
        return ResponseEntity.ok(ApiResponse.<LeaveRequest>builder()
                .success(true)
                .message("Leave approved")
                .data(request)
                .build());
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/leave-requests/{leaveRequestId}/reject")
    public ResponseEntity<ApiResponse<LeaveRequest>> rejectRequest(
            @PathVariable("leaveRequestId") Long leaveRequestId,
            @RequestParam  String comment
    ) {
        LeaveRequest request = adminService.reject(leaveRequestId, comment);
        return ResponseEntity.ok(ApiResponse.<LeaveRequest>builder()
                .success(true)
                .message("Leave rejected")
                .data(request)
                .build());
    }



    //  View Pending Requests
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/leave-requests/pending")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> pendingRequests() {
        List<LeaveRequest> requests = adminService.getPendingRequests();
        return ResponseEntity.ok(ApiResponse.<List<LeaveRequest>>builder()
                .success(true)
                .message("Pending leave requests")
                .data(requests)
                .build());
    }

    @PostMapping("/admin/leave/init-balance/{userId}")
    public ResponseEntity<ApiResponse> initLeaveBalance(@PathVariable String userId) {
        leaveBalanceService.initializeLeaveBalanceForUser(userId);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Leave balances initialized for user")
                        .build()
        );
    }


    //  Generate Reports
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReportDto>>> reports() {
        List<ReportDto> reports = reportingService.generateAllLeaveReports();
        return ResponseEntity.ok(ApiResponse.<List<ReportDto>>builder()
                .success(true)
                .message("Leave report generated")
                .data(reports)
                .build());
    }
}
