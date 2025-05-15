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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.christabella.africahr.leavemanagement.entity.LeaveBalance;
import com.christabella.africahr.leavemanagement.exception.LeaveBalanceExceededException;
import com.christabella.africahr.leavemanagement.repository.LeaveBalanceRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import com.christabella.africahr.leavemanagement.entity.PublicHoliday;
import com.christabella.africahr.leavemanagement.repository.PublicHolidayRepository;

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
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final PublicHolidayRepository publicHolidayRepository;

    public long calculateBusinessDays(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> holidays = publicHolidayRepository.findByDateBetween(startDate, endDate)
                .stream()
                .map(PublicHoliday::getDate)
                .collect(Collectors.toList());

        long days = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (!(currentDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    currentDate.getDayOfWeek() == DayOfWeek.SUNDAY ||
                    holidays.contains(currentDate))) {
                days++;
            }
            currentDate = currentDate.plusDays(1);
        }

        return days;
    }

    public LeaveRequest applyForLeave(LeaveRequestDto dto, MultipartFile file) {
        String username = getCurrentUserId();
        String userId = userServiceClient.getUserIdByEmail(username);

        if (userId == null) {
            log.error("userId is null for email: {}", username);
            throw new BadRequestException("Unable to find user ID for email: " + username);
        }

        if (!isValidUUID(userId)) {
            log.error("userId is not a valid UUID: {}", userId);
            throw new IllegalArgumentException("userId must be a UUID, not an email or invalid string");
        }

        String email = userServiceClient.getUserEmail(userId);

        boolean hasPending = leaveRequestRepository.existsByUserIdAndStatus(userId, LeaveStatus.PENDING);
        if (hasPending) {
            throw new BadRequestException(
                    "You already have a pending leave request. Please wait for it to be approved or rejected.");
        }

        LeaveType leaveType = leaveTypeRepository.findByNameIgnoreCase(dto.getLeaveTypeName())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid leave type"));

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        long requestedDays = calculateBusinessDays(dto.getStartDate(), dto.getEndDate());

        if (requestedDays <= 0) {
            throw new BadRequestException("Leave request must include at least one business day");
        }

        int currentYear = LocalDate.now().getYear();

        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveType_IdAndYear(userId, leaveType.getId(), currentYear)
                .orElseGet(() -> {
                    leaveBalanceService.initializeLeaveBalanceForUser(userId, currentYear);
                    return leaveBalanceRepository
                            .findByUserIdAndLeaveType_IdAndYear(userId, leaveType.getId(), currentYear)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Leave balance not found after initialization"));
                });

        double availableBalance = Double.parseDouble(balance.getRemainingLeave());

        if (availableBalance < requestedDays) {
            String message = String.format(
                    "Unable to process request: %s has a maximum annual allocation of %.1f days. " +
                            "You currently have %.1f days available and are requesting %d days. " +
                            "Please adjust your leave dates to stay within your available balance.",
                    leaveType.getName(), balance.getDefaultBalance(), availableBalance, requestedDays);

            throw new LeaveBalanceExceededException(message);
        }

        String documentUrl = (file != null && !file.isEmpty()) ? documentService.storeFile(file) : null;

        LeaveRequest request = LeaveRequest.builder()
                .userId(userId)
                .leaveType(leaveType)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .documentUrl(documentUrl)
                .status(LeaveStatus.PENDING)
                .email(email)
                .build();
        request = leaveRequestRepository.save(request);

        try {
            sendEmailNotifications(request);
            notificationService.notify(userId, "Your leave request has been submitted.");
        } catch (Exception e) {
            log.error("Failed to send notifications for leave request ID {}: {}", request.getId(), e.getMessage(), e);
        }

        return request;
    }

    private boolean isValidUUID(String userId) {
        try {
            java.util.UUID.fromString(userId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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

    private void sendEmailNotifications(LeaveRequest leaveRequest) {
        try {
            String userEmail = userServiceClient.getUserEmail(leaveRequest.getUserId());
            if (userEmail == null || userEmail.isBlank()) {
                log.warn("User email is null or blank for userId: {}", leaveRequest.getUserId());
                return;
            }

            Set<String> approverEmails = new HashSet<>();

            List<String> managers = userServiceClient.getUsersByRole("MANAGER");
            List<String> admins = userServiceClient.getUsersByRole("ADMIN");

            for (String userId : managers) {
                String email = userServiceClient.getUserEmail(userId);
                if (email != null && !email.isBlank()) {
                    approverEmails.add(email);
                } else {
                    log.warn("Manager email is missing for userId: {}. Skipping email notification.", userId);
                }
            }

            for (String userId : admins) {
                String email = userServiceClient.getUserEmail(userId);
                if (email != null && !email.isBlank()) {
                    approverEmails.add(email);
                } else {
                    log.warn("Admin email is missing for userId: {}. Skipping email notification.", userId);
                }
            }

            Map<String, Object> userModel = Map.of(
                    "name", userServiceClient.getUserFullName(leaveRequest.getUserId()),
                    "startDate", leaveRequest.getStartDate(),
                    "endDate", leaveRequest.getEndDate(),
                    "status", "SUBMITTED");
            emailService.sendHtmlEmail(
                    userEmail,
                    "Leave Request Submitted",
                    "leave-notification",
                    userModel);

            for (String approverEmail : approverEmails) {
                Map<String, Object> approverModel = Map.of(
                        "name", userServiceClient.getUserFullName(leaveRequest.getUserId()),
                        "startDate", leaveRequest.getStartDate(),
                        "endDate", leaveRequest.getEndDate(),
                        "status", "PENDING");
                emailService.sendHtmlEmail(
                        approverEmail,
                        "New Leave Request for Approval",
                        "leave-notification",
                        approverModel);
            }
        } catch (Exception e) {
            log.error("Failed to send email notifications: {}", e.getMessage(), e);
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
                                "status", "UPCOMING"));
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
                .filter(req -> department == null
                        || department.equalsIgnoreCase(userServiceClient.getUserDepartment(req.getUserId())))
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
        try {
            log.info("Fetching all managers and admins");
            List<String> all = new java.util.ArrayList<>();

            List<String> managers = userServiceClient.getAllManagers();
            if (managers != null && !managers.isEmpty()) {
                log.info("Found {} managers", managers.size());
                all.addAll(managers);
            }

            List<String> admins = userServiceClient.getAdmins();
            if (admins != null && !admins.isEmpty()) {
                log.info("Found {} admins", admins.size());
                all.addAll(admins);
            }

            log.info("Total approvers to notify: {}", all.size());
            return all;
        } catch (Exception e) {
            log.error("Error fetching managers and admins: {}", e.getMessage(), e);
            return List.of();
        }
    }

}
