package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.entity.LeaveBalance;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.enums.LeaveStatus;
import com.christabella.africahr.leavemanagement.exception.BadRequestException;
import com.christabella.africahr.leavemanagement.exception.LeaveBalanceExceededException;
import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
import com.christabella.africahr.leavemanagement.repository.LeaveBalanceRepository;
import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
import com.christabella.africahr.leavemanagement.repository.PublicHolidayRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.stream.Collectors;
import com.christabella.africahr.leavemanagement.dto.LeaveRequestDto;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmailService emailService;
    private final UserServiceClient userServiceClient;
    private final NotificationService notificationService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveService leaveService;

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);


    public List<LeaveRequest> getPendingRequests() {
        List<LeaveRequest> pending = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
        return pending != null ? pending : Collections.emptyList();
    }


    public List<LeaveRequestDto> getPendingRequestDtos() {
        List<LeaveRequest> pending = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
        if (pending == null || pending.isEmpty()) {
            return Collections.emptyList();
        }
        return pending.stream().map(req -> {
            logger.info("Fetching email for userId: {}", req.getUserId());
            String email = "N/A";
            if (req.getUserId() != null) {
                email = userServiceClient.getUserEmail(req.getUserId());
                logger.info("Fetched email for userId {}: {}", req.getUserId(), email);
            } else {
                logger.warn("LeaveRequest ID {} has null userId!", req.getId());
            }
            String role = userServiceClient.getUserRole(req.getUserId());
            return LeaveRequestDto.builder()
                    .id(req.getId())
                    .leaveTypeName(req.getLeaveType().getName())
                    .startDate(req.getStartDate())
                    .endDate(req.getEndDate())
                    .reason(req.getReason())
                    .documentUrl(req.getDocumentUrl())
                    .email(email)
                    .role(role)
                    .build();
        }).collect(Collectors.toList());
    }
    

    // Approve leave request
    @Transactional
    public LeaveRequest approve(Long requestId, String comment) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getStatus().equals(LeaveStatus.PENDING)) {
            throw new BadRequestException("Only pending requests can be approved");
        }

        int currentYear = LocalDate.now().getYear();
        
        // Use LeaveService's calculateBusinessDays method
        long businessDays = leaveService.calculateBusinessDays(request.getStartDate(), request.getEndDate());
        
        logger.info("Calculated {} business days for leave from {} to {}", 
                businessDays, request.getStartDate(), request.getEndDate());


        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveType_IdAndYear(request.getUserId(), request.getLeaveType().getId(), currentYear)
                .orElse(null);

        if (balance == null) {
            leaveBalanceService.initializeLeaveBalanceForUser(request.getUserId(), currentYear);
            balance = leaveBalanceRepository
                    .findByUserIdAndLeaveType_IdAndYear(request.getUserId(), request.getLeaveType().getId(), currentYear)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found after initialization"));
        }


        double availableBalance = Double.parseDouble(balance.getRemainingLeave());
        
        if (availableBalance < businessDays) {
            String message = String.format(
                    "Insufficient leave balance. User has %.1f days available but requested %d business days.",
                    availableBalance, businessDays);
            throw new LeaveBalanceExceededException(message);
        }

        logger.info("[APPROVE] Before deduction: userId={}, leaveTypeId={}, year={}, availableBalance={}, usedLeave={}",
                request.getUserId(), request.getLeaveType().getId(), currentYear, availableBalance,
                balance.getUsedLeave());

        try {
            request.setStatus(LeaveStatus.APPROVED);
            request.setApproverComment(comment);
            
            leaveBalanceService.updateBalanceForApprovedLeave(request.getUserId(), request.getLeaveType().getId(),
                    businessDays);
            logger.info("APPROVE - BALANCE UPDATE SUCCEEDED");
            
            LeaveBalance verifiedBalance = leaveBalanceRepository
                    .findByUserIdAndLeaveType_IdAndYear(request.getUserId(), request.getLeaveType().getId(), currentYear)
                    .orElse(null);
                    
            if (verifiedBalance != null) {
                logger.info("[APPROVE] After update - DefaultBalance: {}, UsedLeave: {}, RemainingLeave: {}",
                        verifiedBalance.getDefaultBalance(),
                        verifiedBalance.getUsedLeave(),
                        verifiedBalance.getRemainingLeave());
            } else {
                logger.error("[APPROVE] Failed to verify balance in DB after update!");
            }
        } catch (Exception e) {
            logger.error("APPROVE - BALANCE UPDATE FAILED: {}", e.getMessage(), e);
            throw e;
        }

        // Send notifications
        sendEmailNotification(request, "APPROVED");
        notificationService.notify(request.getUserId(), "Your leave request has been approved.");

        return leaveRequestRepository.save(request);
    }

    // Reject leave request
    public LeaveRequest reject(Long requestId, String comment) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        request.setStatus(LeaveStatus.REJECTED);
        request.setApproverComment(comment);

        sendEmailNotification(request, "REJECTED");
        notificationService.notify(request.getUserId(), "Your leave request has been rejected.");

        return leaveRequestRepository.save(request);
    }

    private void sendEmailNotification(LeaveRequest request, String status) {
        String userId = request.getUserId();
        String recipientEmail = userServiceClient.getUserEmail(userId);
        String fullName = userServiceClient.getUserFullName(userId);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            logger.warn("Recipient email is null or blank for userId: {}", userId);
            return;
        }

        Map<String, Object> model = new java.util.HashMap<>();
        model.put("name", fullName != null ? fullName : "");
        model.put("startDate", request.getStartDate() != null ? request.getStartDate() : "");
        model.put("endDate", request.getEndDate() != null ? request.getEndDate() : "");
        model.put("status", status != null ? status : "");

        logger.info("Sending email to: {}", recipientEmail);

        emailService.sendHtmlEmail(
                recipientEmail,
                "Your Leave Request has been " + status,
                "leave-notification",
                model);
    }
}