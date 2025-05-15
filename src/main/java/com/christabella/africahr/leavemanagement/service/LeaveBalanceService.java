package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.dto.LeaveBalanceDto;
import com.christabella.africahr.leavemanagement.entity.LeaveBalance;
import com.christabella.africahr.leavemanagement.entity.LeaveType;
import com.christabella.africahr.leavemanagement.enums.LeaveStatus;
import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
import com.christabella.africahr.leavemanagement.repository.LeaveBalanceRepository;
import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
import com.christabella.africahr.leavemanagement.repository.LeaveTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserServiceClient userServiceClient;

    @Scheduled(cron = "0 0 0 1 * *", zone = "Africa/Kigali")
    @Transactional
    public void accrueMonthlyLeave() {
        int currentYear = LocalDate.now().getYear();
        log.info("Running monthly leave accrual for year: {}", currentYear);

        List<LeaveBalance> balances = leaveBalanceRepository.findAll();

        for (LeaveBalance balance : balances) {

            if (balance.getYear() != currentYear) {
                continue;
            }

            if (balance.getLeaveType().getName().toLowerCase().contains("personal") ||
                    balance.getLeaveType().getName().toLowerCase().contains("pto") ||
                    balance.getLeaveType().getName().toLowerCase().contains("annual")) {

                double remaining = Double.parseDouble(balance.getRemainingLeave());

                double newBalance = remaining + 1.66;

                double maxAllocation = balance.getLeaveType().getDefaultBalance();
                if (newBalance > maxAllocation) {
                    newBalance = maxAllocation;
                }

                balance.setRemainingLeave(
                        newBalance % 1 == 0 ? String.format("%.0f", newBalance) : String.format("%.1f", newBalance));
                log.info("Accrued monthly leave for user: {}, new balance: {}, leave type: {}",
                        balance.getUserId(), newBalance, balance.getLeaveType().getName());
            }
        }

        leaveBalanceRepository.saveAll(balances);
        log.info("Monthly leave accrual completed for year {}", currentYear);
    }

    @Scheduled(cron = "0 0 1 1 1 *", zone = "Africa/Kigali")
    @Transactional
    public void carryOverLogic() {
        int previousYear = LocalDate.now().getYear() - 1;
        int currentYear = LocalDate.now().getYear();

        log.info("Running year-end carryover from {} to {}", previousYear, currentYear);

        List<LeaveBalance> previousYearBalances = leaveBalanceRepository.findByYear(previousYear);

        for (LeaveBalance prevBalance : previousYearBalances) {

            if (prevBalance.getLeaveType().getName().toLowerCase().contains("personal") ||
                    prevBalance.getLeaveType().getName().toLowerCase().contains("pto") ||
                    prevBalance.getLeaveType().getName().toLowerCase().contains("annual")) {

                double remainingBalance = Double.parseDouble(prevBalance.getRemainingLeave());

                double carryOver = Math.min(remainingBalance, 5.0);

                log.info("Processing carryover for user: {}, leave type: {}, remaining: {}, carryover: {}",
                        prevBalance.getUserId(), prevBalance.getLeaveType().getName(),
                        remainingBalance, carryOver);

                LeaveBalance currentYearBalance = leaveBalanceRepository
                        .findByUserIdAndLeaveType_IdAndYear(prevBalance.getUserId(),
                                prevBalance.getLeaveType().getId(),
                                currentYear)
                        .orElse(null);

                if (currentYearBalance == null) {

                    currentYearBalance = LeaveBalance.builder()
                            .userId(prevBalance.getUserId())
                            .userEmail(prevBalance.getUserEmail())
                            .leaveType(prevBalance.getLeaveType())
                            .defaultBalance(prevBalance.getLeaveType().getDefaultBalance())
                            .carryOver(carryOver)
                            .usedLeave(0.0)
                            .remainingLeave((prevBalance.getLeaveType().getDefaultBalance() + carryOver) % 1 == 0
                                    ? String.format("%.0f", prevBalance.getLeaveType().getDefaultBalance() + carryOver)
                                    : String.format("%.1f", prevBalance.getLeaveType().getDefaultBalance() + carryOver))
                            .year(currentYear)
                            .build();
                } else {

                    currentYearBalance.setCarryOver(carryOver);
                    double newRemaining = prevBalance.getLeaveType().getDefaultBalance() + carryOver
                            - currentYearBalance.getUsedLeave();
                    currentYearBalance.setRemainingLeave(newRemaining % 1 == 0 ? String.format("%.0f", newRemaining)
                            : String.format("%.1f", newRemaining));
                }

                leaveBalanceRepository.save(currentYearBalance);
            }
        }

        log.info("Year-end carryover processing completed");
    }

    @Transactional
    public List<LeaveBalanceDto> getBalancesByUserId(String userId) {
        int currentYear = LocalDate.now().getYear();

        List<LeaveBalance> existingBalances = leaveBalanceRepository.findByUserIdAndYear(userId, currentYear);

        if (existingBalances.isEmpty()) {
            initializeLeaveBalanceForUser(userId, currentYear);
            existingBalances = leaveBalanceRepository.findByUserIdAndYear(userId, currentYear);
        }

        String userEmail;
        try {
            userEmail = userServiceClient.getUserEmail(userId);
        } catch (Exception e) {
            log.warn("Failed to fetch email, using cached value in DB");
            userEmail = existingBalances.isEmpty() ? "unknown@example.com" : existingBalances.get(0).getUserEmail();
        }

        for (LeaveBalance balance : existingBalances) {
            balance.setUserEmail(userEmail);

            LeaveType leaveType = balance.getLeaveType();

            double calculatedUsedDays = leaveRequestRepository
                    .findByUserIdAndLeaveTypeAndStatusAndStartDateBetween(
                            userId,
                            leaveType,
                            LeaveStatus.APPROVED,
                            LocalDate.of(currentYear, 1, 1),
                            LocalDate.of(currentYear, 12, 31))
                    .stream()
                    .mapToLong(req -> ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1)
                    .sum();

            if (balance.isManuallyAdjusted()) {
                log.info("Using manually adjusted value for {}: {} instead of calculated: {}",
                        balance.getLeaveType().getName(), balance.getUsedLeave(), calculatedUsedDays);

                    } else {

                balance.setUsedLeave(calculatedUsedDays);
            }


            double remaining = balance.getDefaultBalance() + balance.getCarryOver() - balance.getUsedLeave();
            balance.setRemainingLeave(
                    remaining % 1 == 0 ? String.format("%.0f", remaining) : String.format("%.1f", remaining));

            leaveBalanceRepository.save(balance);
        }

        return existingBalances.stream()
                .map(balance -> LeaveBalanceDto.builder()
                        .leaveType(balance.getLeaveType().getName())
                        .leaveTypeId(balance.getLeaveType().getId())
                        .defaultBalance(balance.getDefaultBalance())
                        .usedLeave((int) balance.getUsedLeave())
                        .remainingLeave(balance.getRemainingLeave())
                        .carryOver(balance.getCarryOver())
                        .year(balance.getYear())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void initializeLeaveBalanceForUser(String userId) {
        initializeLeaveBalanceForUser(userId, LocalDate.now().getYear());
    }

    @Transactional
    public void initializeLeaveBalanceForUser(String userId, int year) {
        String correctedUserId = userId;

        if (userId.contains("@")) {
            log.warn("Received email as userId: {}. Converting to UUID...", userId);
            String uuidFromEmail = userServiceClient.getUserIdByEmail(userId);

            if (uuidFromEmail == null || uuidFromEmail.contains("@")) {
                log.error("Failed to get proper UUID for email: {}", userId);
                throw new IllegalArgumentException("User ID must be a UUID");
            }

            correctedUserId = uuidFromEmail;
            log.info("Converted email to UUID: {}", correctedUserId);
        }

        final String finalUserId = correctedUserId;

        List<LeaveBalance> existingBalances = leaveBalanceRepository.findByUserIdAndYear(finalUserId, year);
        if (!existingBalances.isEmpty()) {
            log.info("Balance already exists for user {} in year {}. Skipping initialization.", finalUserId, year);
            return;
        }

        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
        String email = userServiceClient.getUserEmail(finalUserId);

        List<LeaveBalance> balances = leaveTypes.stream()
                .map(leaveType -> {
                    double defaultBalance = leaveType.getDefaultBalance();
                    return LeaveBalance.builder()
                            .userId(finalUserId)
                            .userEmail(email)
                            .leaveType(leaveType)
                            .defaultBalance(defaultBalance)
                            .carryOver(0.0)
                            .usedLeave(0.0)
                            .remainingLeave(defaultBalance % 1 == 0 ? String.format("%.0f", defaultBalance)
                                    : String.format("%.1f", defaultBalance))
                            .year(year)
                            .manuallyAdjusted(false)
                            .build();
                })
                .collect(Collectors.toList());

        leaveBalanceRepository.saveAll(balances);
        log.info("Leave balances initialized for user: {} (email: {}) for year: {}", finalUserId, email, year);
    }

    @Transactional
    public LeaveBalance adjustLeaveBalance(String userId, Long leaveTypeId, double newBalance) {
        int currentYear = LocalDate.now().getYear();

        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveType_IdAndYear(userId, leaveTypeId, currentYear)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found for current year"));

        balance.setDefaultBalance(newBalance);
        balance.setManuallyAdjusted(true);

        double remaining = newBalance + balance.getCarryOver() - balance.getUsedLeave();
        balance.setRemainingLeave(
                remaining % 1 == 0 ? String.format("%.0f", remaining) : String.format("%.1f", remaining));

        return leaveBalanceRepository.save(balance);
    }

    @Transactional
    public void updateBalanceForApprovedLeave(String userId, Long leaveTypeId, long days) {
        try {
            int currentYear = LocalDate.now().getYear();
            log.info("BALANCE UPDATE START: userId={}, leaveTypeId={}, days={}, year={}",
                    userId, leaveTypeId, days, currentYear);

            LeaveBalance balance = leaveBalanceRepository
                    .findByUserIdAndLeaveType_IdAndYear(userId, leaveTypeId, currentYear)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found for current year"));

            log.info("FOUND BALANCE: id={}, defaultBalance={}, carryOver={}, used={}, remaining={}",
                    balance.getId(), balance.getDefaultBalance(), balance.getCarryOver(),
                    balance.getUsedLeave(), balance.getRemainingLeave());

            double currentUsed = balance.getUsedLeave();
            double newUsed = currentUsed + days;
            balance.setUsedLeave(newUsed);

            double remaining = balance.getDefaultBalance() + balance.getCarryOver() - newUsed;
            balance.setRemainingLeave(
                    remaining % 1 == 0 ? String.format("%.0f", remaining) : String.format("%.1f", remaining));

            LeaveBalance updated = leaveBalanceRepository.save(balance);
            log.info("BALANCE UPDATED: id={}, remaining={}, used={}",
                    updated.getId(), updated.getRemainingLeave(), updated.getUsedLeave());
        } catch (Exception e) {
            log.error("ERROR updating balance: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public int initializeAllLeaveBalances() {
        int currentYear = LocalDate.now().getYear();

        List<String> userIds = new ArrayList<>();
        userIds.addAll(userServiceClient.getUsersByRole("STAFF"));
        userIds.addAll(userServiceClient.getUsersByRole("MANAGER"));
        userIds.addAll(userServiceClient.getUsersByRole("ADMIN"));

        Set<String> uniqueUserIds = new HashSet<>(userIds);
        log.info("Initializing leave balances for {} users for year {}", uniqueUserIds.size(), currentYear);

        int successCount = 0;
        for (String userId : uniqueUserIds) {
            try {
                initializeLeaveBalanceForUser(userId, currentYear);
                successCount++;
                log.info("Successfully initialized leave balance for user: {} for year {}", userId, currentYear);
            } catch (Exception e) {
                log.error("Failed to initialize leave balance for user {}: {}", userId, e.getMessage());
            }
        }

        log.info("Completed initializing leave balances. Success: {}/{}", successCount, uniqueUserIds.size());
        return successCount;
    }

    @Transactional
    public LeaveBalance adjustUsedDays(String userId, Long leaveTypeId, Double usedDays) {
        int currentYear = LocalDate.now().getYear();

        log.info("ADJUST_USED_DAYS START: userId={}, leaveTypeId={}, usedDays={}, year={}",
                userId, leaveTypeId, usedDays, currentYear);

        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveType_IdAndYear(userId, leaveTypeId, currentYear)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

        log.info("FOUND BALANCE: id={}, leaveType={}, usedLeave={} -> {}, remainingLeave={}",
                balance.getId(), balance.getLeaveType().getName(),
                balance.getUsedLeave(), usedDays, balance.getRemainingLeave());

        double usedDaysValue = (usedDays != null) ? usedDays : 0.0;

        balance.setUsedLeave(usedDaysValue);
        balance.setManuallyAdjusted(true);

        double remaining = balance.getDefaultBalance() + balance.getCarryOver() - usedDaysValue;
        String newRemainingValue = remaining % 1 == 0 ? String.format("%.0f", remaining)
                : String.format("%.1f", remaining);
        balance.setRemainingLeave(newRemainingValue);

        LeaveBalance updated = leaveBalanceRepository.saveAndFlush(balance);

        leaveBalanceRepository.flush();

        log.info("ADJUST_USED_DAYS SUCCESS: id={}, leaveType={}, usedLeave={}, remainingLeave={}",
                updated.getId(), updated.getLeaveType().getName(),
                updated.getUsedLeave(), updated.getRemainingLeave());

        return updated;
    }

    public LeaveBalance getLeaveBalanceOrNull(String userId, Long leaveTypeId) {
        int currentYear = LocalDate.now().getYear();

        try {
            return leaveBalanceRepository
                    .findByUserIdAndLeaveType_IdAndYear(userId, leaveTypeId, currentYear)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error fetching leave balance: {}", e.getMessage());
            return null;
        }
    }
}