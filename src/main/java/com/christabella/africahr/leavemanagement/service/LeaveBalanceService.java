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

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private  final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;


    @Scheduled(cron = "0 0 1 1 * *", zone = "Africa/Kigali")
    @Transactional
    public void accrueMonthlyLeave() {
        List<LeaveBalance> balances = leaveBalanceRepository.findAll();
        for (LeaveBalance balance : balances) {

            if ("Personal Time Off".equalsIgnoreCase(balance.getLeaveType().getName())) {
                balance.setBalance(balance.getBalance() + 1.66);
            }
        }
        leaveBalanceRepository.saveAll(balances);
        log.info("✅ Monthly leave accrual executed successfully");
    }



    @Scheduled(cron = "0 0 2 1 1 *", zone = "Africa/Kigali")
    @Transactional
    public void carryOverLogic() {
        List<LeaveBalance> balances = leaveBalanceRepository.findAll();
        for (LeaveBalance balance : balances) {
            if ("Personal Time Off".equalsIgnoreCase(balance.getLeaveType().getName())) {
                double carryOver = Math.min(balance.getBalance(), 5.0);
                balance.setBalance(carryOver);
                balance.setCarryOver(carryOver);
            }
        }
        leaveBalanceRepository.saveAll(balances);
        log.info("✅ Year-end carryover logic executed");
    }



    public List<LeaveBalanceDto> getBalancesByUserId(String userId) {
        List<LeaveBalance> existingBalances = leaveBalanceRepository.findByUserId(userId);

        if (existingBalances.isEmpty()) {
            initializeLeaveBalanceForUser(userId);
            existingBalances = leaveBalanceRepository.findByUserId(userId);
        }

        return existingBalances.stream()
                .map(balance -> {
                    LeaveType leaveType = balance.getLeaveType();


                    long usedLeave = leaveRequestRepository
                            .findByUserIdAndLeaveTypeAndStatus(userId, leaveType, LeaveStatus.APPROVED)
                            .stream()
                            .mapToLong(req -> ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1)
                            .sum();

                    return LeaveBalanceDto.builder()
                            .leaveType(leaveType.getName())
                            .defaultBalance(leaveType.getDefaultBalance())
                            .balance(balance.getBalance())
                            .carryOver(balance.getCarryOver())
                            .usedLeave((int) usedLeave)
                            .build();
                })
                .collect(Collectors.toList());
    }



    public void initializeLeaveBalanceForUser(String userId) {
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();

        List<LeaveBalance> balances = leaveTypes.stream()
                .map(leaveType -> LeaveBalance.builder()
                        .userId(userId)
                        .leaveType(leaveType)
                        .balance(leaveType.getDefaultBalance())
                        .carryOver(0.0)
                        .build())
                .collect(Collectors.toList());

        leaveBalanceRepository.saveAll(balances);
    }



    public LeaveBalance adjustLeaveBalance(String userId, Long leaveTypeId, double newBalance) {
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveType_Id(userId, leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

        balance.setBalance(newBalance);
        return leaveBalanceRepository.save(balance);
    }


}
