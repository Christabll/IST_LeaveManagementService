    package com.christabella.africahr.leavemanagement.service;

    import com.christabella.africahr.leavemanagement.entity.LeaveBalance;
    import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
    import com.christabella.africahr.leavemanagement.entity.LeaveType;
    import com.christabella.africahr.leavemanagement.enums.LeaveStatus;
    import com.christabella.africahr.leavemanagement.exception.BadRequestException;
    import com.christabella.africahr.leavemanagement.exception.LeaveBalanceExceededException;
    import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
    import com.christabella.africahr.leavemanagement.repository.LeaveBalanceRepository;
    import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
    import com.christabella.africahr.leavemanagement.repository.LeaveTypeRepository;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;

    import java.time.temporal.ChronoUnit;
    import java.util.List;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class AdminService {

        private final LeaveRequestRepository leaveRequestRepository;
        private final LeaveTypeRepository leaveTypeRepository;
        private final LeaveBalanceRepository leaveBalanceRepository;

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

            return leaveRequestRepository.save(request);
        }



        public LeaveRequest reject(Long requestId, String comment) {
            LeaveRequest request = leaveRequestRepository.findById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
            request.setStatus(LeaveStatus.REJECTED);
            request.setApproverComment(comment);
            return leaveRequestRepository.save(request);
        }



    }
