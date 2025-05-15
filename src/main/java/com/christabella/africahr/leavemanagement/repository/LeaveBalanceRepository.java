package com.christabella.africahr.leavemanagement.repository;

import com.christabella.africahr.leavemanagement.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;


public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByUserId(String userId);
    
    List<LeaveBalance> findByUserIdAndYear(String userId, int year);
    
    List<LeaveBalance> findByYear(int year);
    
    Optional<LeaveBalance> findByUserIdAndLeaveType_Id(String userId, Long leaveTypeId);
    
    Optional<LeaveBalance> findByUserIdAndLeaveType_IdAndYear(String userId, Long leaveTypeId, int year);
}

