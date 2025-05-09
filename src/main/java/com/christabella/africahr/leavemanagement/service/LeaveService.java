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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
        String userId = getCurrentUserId();

        boolean hasPending = leaveRequestRepository.existsByUserIdAndStatus(userId, LeaveStatus.PENDING);
        if (hasPending) {
            throw new BadRequestException("You already have a pending leave request. Please wait for it to be approved or rejected.");
        }


        LeaveType leaveType = leaveTypeRepository.findByNameIgnoreCase(dto.getLeaveTypeName())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid leave type"));

        String documentUrl = (file != null && !file.isEmpty()) ? documentService.storeFile(file) : null;

        LeaveRequest request = LeaveRequest.builder()
                .userId(userId)
                .leaveType(leaveType)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .documentUrl(documentUrl)
                .status(LeaveStatus.PENDING)
                .build();
        log.info("Saving leave request with status: {}", request.getStatus());
        request = leaveRequestRepository.save(request);

        try {
            sendLeaveSubmissionEmail(request);
            notificationService.notify(userId, "Your leave request has been submitted.");
            for (String approverId : getAllManagersAndAdmins()) {
                notifyApprover(approverId, request);
            }
        } catch (Exception e) {
            log.error("Notification error: ", e);
        }

        return request;
    }



    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("User is not authenticated.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        throw new BadRequestException("Unable to extract user ID from authentication.");
    }

    private void sendLeaveSubmissionEmail(LeaveRequest request) {
        if (request == null || request.getUserId() == null) {
            log.warn("Request or userId is null. Skipping email.");
            return;
        }

        String userId = request.getUserId();
        try {
            String recipientEmail = userServiceClient.getUserEmail(userId);
            String fullName = userServiceClient.getUserFullName(userId);

            if (recipientEmail == null || fullName == null) {
                log.warn("User email or name is null for userId: {}", userId);
                return;
            }

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
            log.info("Leave submission email sent to {}", recipientEmail);

        } catch (Exception e) {
            log.error("Error sending leave submission email to userId: {}", userId, e);
        }
    }


    private void notifyApprover(String approverId, LeaveRequest request) {
        try {
            String email = userServiceClient.getUserEmail(approverId);
            String fullName = userServiceClient.getUserFullName(approverId);

            if (email == null || email.isBlank() || fullName == null || fullName.isBlank()) {
                log.warn("Approver {} email or name is missing. Skipping email notification.", approverId);
                return;
            }

            Map<String, Object> model = Map.of(
                    "name", fullName,
                    "startDate", request.getStartDate(),
                    "endDate", request.getEndDate(),
                    "status", "PENDING"
            );

            notificationService.notify(approverId, "A new leave request has been submitted.");
            emailService.sendHtmlEmail(
                    email,
                    "New Leave Request",
                    "leave-notification",
                    model
            );
            log.info("Leave request email sent to approver {}", email);

        } catch (Exception e) {
            log.error("Failed to send leave request email to approver: {}", approverId, e);
        }
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Africa/Kigali")
    public void remindUsersOfUpcomingLeaves() {
        List<LeaveRequest> upcoming = leaveRequestRepository.findByStartDate(LocalDate.now().plusDays(1));
        for (LeaveRequest request : upcoming) {
            String userId = request.getUserId();
            notificationService.notify(userId, "Reminder: Your leave starts tomorrow.");
            try {
                emailService.sendHtmlEmail(
                        getUserEmail(userId),
                        "Upcoming Leave Reminder",
                        "leave-notification",
                        Map.of(
                                "name", getUserFullName(userId),
                                "startDate", request.getStartDate(),
                                "endDate", request.getEndDate(),
                                "status", "UPCOMING"
                        )
                );
            } catch (Exception e) {
                log.error("Failed to send upcoming leave reminder", e);
            }
        }
    }

    public List<LeaveRequest> getMyLeaveRequests() {
        String userId = getCurrentUserId();
        List<LeaveRequest> requests = leaveRequestRepository.findByUserId(userId);
        if (requests.isEmpty()) {
            throw new ResourceNotFoundException("You have not submitted any leave requests.");
        }
        return requests;
    }

    public ApiResponse<List<TeamOnLeaveDto>> getTeamOnLeaveTooltipInfo(String department) {
        List<LeaveRequest> approved = leaveRequestRepository.findByStatus(LeaveStatus.APPROVED);
        if (approved.isEmpty()) {
            throw new ResourceNotFoundException("No team member is currently on leave.");
        }

        List<TeamOnLeaveDto> result = approved.stream()
                .filter(req -> department == null || department.equalsIgnoreCase(userServiceClient.getUserDepartment(req.getUserId())))
                .map(req -> TeamOnLeaveDto.builder()
                        .fullName(userServiceClient.getUserFullName(req.getUserId()))
                        .avatarUrl(userServiceClient.getUserAvatar(req.getUserId()))
                        .leaveUntil(req.getEndDate())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.<List<TeamOnLeaveDto>>builder()
                .success(true)
                .message("Team members currently on leave with details")
                .data(result)
                .build();
    }

    private String getUserEmail(String userId) {
        return userServiceClient.getUserEmail(userId);
    }

    private String getUserFullName(String userId) {
        return userServiceClient.getUserFullName(userId);
    }



    private List<String> getAllManagersAndAdmins() {
        String department = userServiceClient.getUserDepartment(getCurrentUserId());
        List<String> all = new java.util.ArrayList<>(userServiceClient.getManagers(department));
        all.addAll(userServiceClient.getAdmins());
        return all;
    }
}
