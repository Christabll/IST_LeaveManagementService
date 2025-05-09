package com.christabella.africahr.leavemanagement.repository;

import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.entity.LeaveType;
import com.christabella.africahr.leavemanagement.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserId(String userId);
    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByUserIdAndLeaveTypeAndStatus(String userId, LeaveType leaveType, LeaveStatus status);
    @Query("SELECT r FROM LeaveRequest r WHERE r.startDate = :startDate AND r.status = 'APPROVED'")
    List<LeaveRequest> findByStartDate(@Param("startDate") LocalDate startDate);
    boolean existsByUserIdAndStatus(String userId, LeaveStatus status);


}
