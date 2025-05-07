package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.dto.ApiResponse;
import com.christabella.africahr.leavemanagement.dto.LeaveRequestDto;
import com.christabella.africahr.leavemanagement.dto.TeamOnLeaveDto;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.entity.LeaveType;
import com.christabella.africahr.leavemanagement.enums.LeaveStatus;
import com.christabella.africahr.leavemanagement.exception.BadRequestException;
import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
import com.christabella.africahr.leavemanagement.repository.LeaveTypeRepository;
import com.christabella.africahr.leavemanagement.security.CustomUserDetails;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmailService emailService;
    private final UserServiceClient userServiceClient;
    private final NotificationService notificationService;
    private final DocumentService documentService;



    public LeaveRequest applyForLeave(LeaveRequestDto dto, MultipartFile file) {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = userDetails.getId();

        boolean conflict = leaveRequestRepository.hasConflictingLeave(
                userId, dto.getStartDate(), dto.getEndDate()
        );
        if (conflict) {
            throw new BadRequestException("You already have a pending leave request for this period.");
        }

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid leave type"));


        if (documentService.requiresDocument(leaveType.getName()) && (file == null || file.isEmpty())) {
            throw new BadRequestException("This leave type requires a supporting document.");
        }


        String documentUrl = null;
        if (file != null && !file.isEmpty()) {
            documentUrl = documentService.storeFile(file);
        }

        LeaveRequest request = LeaveRequest.builder()
                .userId(userId)
                .leaveType(leaveType)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .documentUrl(documentUrl)
                .status(LeaveStatus.PENDING)
                .build();

        request = leaveRequestRepository.save(request);

        sendLeaveSubmissionEmail(request);
        notificationService.notify(userId, "Your leave request has been submitted.");

        return request;
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



    public ApiResponse<List<TeamOnLeaveDto>> getTeamOnLeaveTooltipInfo(@Nullable String department) {
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByStatus(LeaveStatus.APPROVED);

        if (approvedLeaves.isEmpty()) {
            throw new ResourceNotFoundException("No team member is currently on leave.");
        }

        List<TeamOnLeaveDto> result = approvedLeaves.stream()
                .filter(request -> {
                    if (department == null) return true;
                    String userDept = userServiceClient.getUserDepartment(request.getUserId());
                    return department.equalsIgnoreCase(userDept);
                })
                .map(request -> {
                    String fullName = userServiceClient.getUserFullName(request.getUserId());
                    String avatar = userServiceClient.getUserAvatar(request.getUserId());

                    return TeamOnLeaveDto.builder()
                            .fullName(fullName)
                            .avatarUrl(avatar)
                            .leaveUntil(request.getEndDate())
                            .build();
                })
                .collect(Collectors.toList());

        return ApiResponse.<List<TeamOnLeaveDto>>builder()
                .success(true)
                .message("Team members currently on leave with details")
                .data(result)
                .build();
    }



    private void sendLeaveSubmissionEmail(LeaveRequest request) {
        String userId = request.getUserId();
        String recipientEmail = userServiceClient.getUserEmail(userId);
        String fullName = userServiceClient.getUserFullName(userId);

        Map<String, Object> model = Map.of(
                "name", fullName,
                "startDate", request.getStartDate(),
                "endDate", request.getEndDate(),
                "status", "SUBMITTED"
        );

        emailService.sendHtmlEmail(
                recipientEmail,
                "Your Leave Request has been submitted",
                "leave-notification",
                model
        );
    }
}
