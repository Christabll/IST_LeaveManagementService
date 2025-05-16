package com.christabella.africahr.leavemanagement.controller;

import com.christabella.africahr.leavemanagement.dto.*;
import com.christabella.africahr.leavemanagement.entity.LeaveBalance;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.entity.LeaveType;
import com.christabella.africahr.leavemanagement.service.*;
import com.christabella.africahr.leavemanagement.dto.BalanceAdjustmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.christabella.africahr.leavemanagement.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.List;


@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

        private final LeaveTypeService leaveTypeService;
        private final AdminService adminService;
        private final ReportingService reportingService;
        private final LeaveBalanceService leaveBalanceService;
        private final EmailService emailService;
        private static final Logger log = LoggerFactory.getLogger(AdminController.class);

        @PreAuthorize("hasAuthority('ADMIN')")
        @PostMapping("/leave-types")
        public ResponseEntity<ApiResponse<LeaveType>> createLeaveType(@RequestBody LeaveTypeDto dto) {
                LeaveType type = leaveTypeService.createLeaveType(dto);
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                ApiResponse.<LeaveType>builder()
                                                .success(true)
                                                .message("Leave type created")
                                                .data(type)
                                                .build());
        }

        @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
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

        @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
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

        @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
        @GetMapping("/leave-requests/pending")
        public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> pendingRequests() {
                List<LeaveRequestDto> requests = adminService.getPendingRequestDtos();
                return ResponseEntity.ok(ApiResponse.<List<LeaveRequestDto>>builder()
                                .success(true)
                                .message("Pending leave requests")
                                .data(requests)
                                .build());
        }

        @PreAuthorize("hasAuthority('ADMIN')")
        @PostMapping("/leave/init-balance/{userId}")
        public ResponseEntity<ApiResponse> initLeaveBalance(@PathVariable String userId) {
                leaveBalanceService.initializeLeaveBalanceForUser(userId);
                return ResponseEntity.ok(
                                ApiResponse.builder()
                                                .success(true)
                                                .message("Leave balances initialized for user")
                                                .build());
        }

        @PreAuthorize("hasAuthority('ADMIN')")
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

        
        @PreAuthorize("hasAuthority('ADMIN')")
        @PostMapping("/adjust-balance")
        public ResponseEntity<ApiResponse<LeaveBalance>> adjustBalance(@RequestBody BalanceAdjustmentRequest request) {
            LeaveBalance updated;
            
            if (request.getCarryOver() != null) {
                updated = leaveBalanceService.adjustCarryOver(
                        request.getUserId(), request.getLeaveTypeId(), request.getCarryOver()
                );
            } else if (request.getUsedDays() != null) {
                updated = leaveBalanceService.adjustUsedDays(
                        request.getUserId(), request.getLeaveTypeId(), request.getUsedDays()
                );
            } else if (request.getNewBalance() != null) {
                updated = leaveBalanceService.adjustLeaveBalance(
                        request.getUserId(), request.getLeaveTypeId(), request.getNewBalance()
                );
            } else {
                throw new BadRequestException("Either carryOver, newBalance or usedDays must be provided");
            }
            
            return ResponseEntity.ok(ApiResponse.success("Balance adjusted", updated));
        }

        @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
        @GetMapping("/leave/balance/{userId}")
        public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> viewBalanceForUser(@PathVariable String userId) {
                List<LeaveBalanceDto> balances = leaveBalanceService.getBalancesByUserId(userId);
                return ResponseEntity.ok(ApiResponse.<List<LeaveBalanceDto>>builder()
                                .success(true)
                                .message("User leave balance")
                                .data(balances)
                                .build());
        }

        @PreAuthorize("hasAuthority('ADMIN')")
        @PostMapping("/leave/init-balance-all")
        public ResponseEntity<ApiResponse> initializeAllLeaveBalances() {
                int count = leaveBalanceService.initializeAllLeaveBalances();

                return ResponseEntity.ok(
                                ApiResponse.builder()
                                                .success(true)
                                                .message("Leave balances initialized for " + count + " users")
                                                .build());
        }

        @PreAuthorize("hasAuthority('ADMIN')")
        @PostMapping("/adjust-used-days")
        public ResponseEntity<ApiResponse<LeaveBalance>> adjustUsedDays(@RequestBody UsedDaysAdjustmentRequest request) {
            try {
                log.info("CONTROLLER: Adjust used days request details: userId={}, leaveTypeId={}, usedDays={}",
                    request.getUserId(), request.getLeaveTypeId(), request.getUsedDays());
            

                    LeaveBalance beforeChange = leaveBalanceService.getLeaveBalanceOrNull(
                    request.getUserId(), request.getLeaveTypeId());
                
                if (beforeChange != null) {
                    log.info("CONTROLLER: Current state in DB: usedLeave={}, remainingLeave={}",
                        beforeChange.getUsedLeave(), beforeChange.getRemainingLeave());
                } else {
                    log.warn("CONTROLLER: No record found in DB before adjustment");
                }
            
                LeaveBalance updated = leaveBalanceService.adjustUsedDays(
                        request.getUserId(), 
                        request.getLeaveTypeId(), 
                        request.getUsedDays());
            
                log.info("CONTROLLER: After adjustment: usedLeave={}, remainingLeave={}",
                    updated.getUsedLeave(), updated.getRemainingLeave());
            
                return ResponseEntity.ok(ApiResponse.success("Used days adjusted", updated));
            } catch (Exception e) {
                log.error("CONTROLLER: Error adjusting used days: {}", e.getMessage(), e);
                throw e;
            }
        }

        @GetMapping("/test-email")
        public ResponseEntity<String> testEmailFunctionality(@RequestParam String email) {
            try {
                log.info("Testing email functionality with email: {}", email);
                boolean success = emailService.testEmailSending(email);
                if (success) {
                    return ResponseEntity.ok("Test email sent successfully to: " + email);
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to send test email. Check server logs for details.");
                }
            } catch (Exception e) {
                log.error("Error testing email functionality: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error testing email: " + e.getMessage());
            }
        }
}