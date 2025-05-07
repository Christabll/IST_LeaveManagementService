    package com.christabella.africahr.leavemanagement.service;

    import com.christabella.africahr.leavemanagement.entity.LeaveBalance;
    import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
    import com.christabella.africahr.leavemanagement.enums.LeaveStatus;
    import com.christabella.africahr.leavemanagement.exception.BadRequestException;
    import com.christabella.africahr.leavemanagement.exception.LeaveBalanceExceededException;
    import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
    import com.christabella.africahr.leavemanagement.repository.LeaveBalanceRepository;
    import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import java.time.temporal.ChronoUnit;
    import java.util.List;
    import java.util.Map;


    @Service
    @RequiredArgsConstructor
    public class AdminService {

        private final LeaveRequestRepository leaveRequestRepository;
        private final LeaveBalanceRepository leaveBalanceRepository;
        private final EmailService emailService;
        private final UserServiceClient userServiceClient;
        private final NotificationService notificationService;


        public List<LeaveRequest> getPendingRequests() {
            List<LeaveRequest> pending = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
            if (pending.isEmpty()) {
                throw new ResourceNotFoundException("No pending leave requests found");
            }
            return pending;
        }



        @Transactional
        public LeaveRequest approve(Long requestId, String comment) {
            LeaveRequest request = leaveRequestRepository.findById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

            if (!request.getStatus().equals(LeaveStatus.PENDING)) {
                throw new BadRequestException("Only pending requests can be approved");
            }

            request.setStatus(LeaveStatus.APPROVED);
            request.setApproverComment(comment);

            LeaveBalance balance = leaveBalanceRepository
                    .findByUserIdAndLeaveType_Id(request.getUserId(), request.getLeaveType().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

            if (balance.getBalance() < days) {
                throw new LeaveBalanceExceededException("Insufficient leave balance");
            }

            balance.setBalance(balance.getBalance() - days);
            leaveBalanceRepository.save(balance);

            sendEmailNotification(request, "APPROVED");
            notificationService.notify(request.getUserId(), "Your leave request has been approved.");

            return leaveRequestRepository.save(request);
        }



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

            Map<String, Object> model = Map.of(
                    "name", fullName,
                    "startDate", request.getStartDate(),
                    "endDate", request.getEndDate(),
                    "status", status
            );

            emailService.sendHtmlEmail(
                    recipientEmail,
                    "Your Leave Request has been " + status,
                    "leave-notification",
                    model
            );
        }

    }
