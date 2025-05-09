package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.dto.*;
import com.christabella.africahr.leavemanagement.entity.LeaveBalance;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.entity.LeaveType;
import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
import com.christabella.africahr.leavemanagement.service.*;
import com.christabella.africahr.leavemanagement.dto.BalanceAdjustmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LeaveTypeService leaveTypeService;
    private final AdminService adminService;
    private final ReportingService reportingService;
    private final LeaveBalanceService leaveBalanceService;



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


    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
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


    @GetMapping("/admin/reports/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportReports() {
        ByteArrayInputStream stream = reportingService.exportReportsToCSV();
        InputStreamResource resource = new InputStreamResource(stream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leave-reports.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }


    @PostMapping("/admin/adjust-balance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveBalance>> adjustBalance(@RequestBody BalanceAdjustmentRequest request) {
        LeaveBalance updated = leaveBalanceService.adjustLeaveBalance(
                request.getUserId(), request.getLeaveTypeId(), request.getNewBalance()
        );
        return ResponseEntity.ok(ApiResponse.success("Balance adjusted", updated));
    }


}
