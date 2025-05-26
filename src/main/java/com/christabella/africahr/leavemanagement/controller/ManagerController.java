package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.dto.*;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.service.AdminService;
import com.christabella.africahr.leavemanagement.service.LeaveBalanceService;
import com.christabella.africahr.leavemanagement.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
public class ManagerController {

    private final AdminService adminService;
    private final LeaveBalanceService leaveBalanceService;
    private final ReportingService reportingService;


    @PutMapping("/leave-requests/{leaveRequestId}/approve")
    public ResponseEntity<ApiResponse<LeaveRequest>> approveRequest(
            @PathVariable("leaveRequestId") Long leaveRequestId,
            @RequestParam String comment) {
        LeaveRequest request = adminService.approve(leaveRequestId, comment);
        return ResponseEntity.ok(ApiResponse.<LeaveRequest>builder()
                .success(true)
                .message("Leave approved")
                .data(request)
                .build());
    }


    @PutMapping("/leave-requests/{leaveRequestId}/reject")
    public ResponseEntity<ApiResponse<LeaveRequest>> rejectRequest(
            @PathVariable("leaveRequestId") Long leaveRequestId,
            @RequestParam String comment) {
        LeaveRequest request = adminService.reject(leaveRequestId, comment);
        return ResponseEntity.ok(ApiResponse.<LeaveRequest>builder()
                .success(true)
                .message("Leave rejected")
                .data(request)
                .build());
    }


    @GetMapping("/leave-requests/pending")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> pendingRequests() {
        List<LeaveRequestDto> requests = adminService.getPendingRequestDtos();
        return ResponseEntity.ok(ApiResponse.<List<LeaveRequestDto>>builder()
                .success(true)
                .message("Pending leave requests")
                .data(requests)
                .build());
    }


    @GetMapping("/leave/balance/{userId}")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> viewBalanceForUser(@PathVariable String userId) {
        List<LeaveBalanceDto> balances = leaveBalanceService.getBalancesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.<List<LeaveBalanceDto>>builder()
                .success(true)
                .message("User leave balance")
                .data(balances)
                .build());
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<LeaveReportDto>>> reports(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        List<LeaveReportDto> reports = reportingService.getLeaveReports(type, department, status, start, end);
        return ResponseEntity.ok(ApiResponse.<List<LeaveReportDto>>builder()
                .success(true)
                .message("Leave report generated")
                .data(reports)
                .build());
    }

}