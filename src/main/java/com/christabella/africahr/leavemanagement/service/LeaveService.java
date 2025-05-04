package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.dto.ApiResponse;
import com.christabella.africahr.leavemanagement.dto.LeaveRequestDto;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.entity.LeaveType;
import com.christabella.africahr.leavemanagement.enums.LeaveStatus;
import com.christabella.africahr.leavemanagement.exception.BadRequestException;
import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
import com.christabella.africahr.leavemanagement.repository.LeaveTypeRepository;
import com.christabella.africahr.leavemanagement.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;


    public LeaveRequest applyForLeave(LeaveRequestDto dto) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = userDetails.getId();


        boolean conflict = leaveRequestRepository.hasConflictingLeave(
                userId, dto.getStartDate(), dto.getEndDate()
        );

        if (conflict) {
            throw new BadRequestException("You already have a pending leave request for this period. Please wait until it is approved or rejected before submitting another.");
        }

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid leave type"));

        LeaveRequest request = LeaveRequest.builder()
                .userId(userId)
                .leaveType(leaveType)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .documentUrl(dto.getDocumentUrl())
                .status(LeaveStatus.PENDING)
                .build();

        return leaveRequestRepository.save(request);
    }


    public List<LeaveRequest> getMyLeaveRequests() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new SecurityException("Invalid authentication");
        }
        String userId = userDetails.getId();

        List<LeaveRequest> myRequests = leaveRequestRepository.findByUserId(userId);

        if (myRequests.isEmpty()) {
            throw new ResourceNotFoundException("You have not submitted any leave requests.");
        }

        return myRequests;
    }



    public ApiResponse<List<LeaveRequest>> getCurrentEmployeesOnLeave() {
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByStatus(LeaveStatus.APPROVED);

        if (approvedLeaves.isEmpty()) {
            throw new ResourceNotFoundException("No team member is currently on leave.");
        }

        return ApiResponse.<List<LeaveRequest>>builder()
                .success(true)
                .message("Team members currently on leave")
                .data(approvedLeaves)
                .build();
    }



}
