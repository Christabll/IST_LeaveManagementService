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
    List<LeaveRequest> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate start, LocalDate end);
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LeaveRequest l " +
            "WHERE l.userId = :userId " +
            "AND l.status <> 'REJECTED' " +
            "AND l.startDate <= :endDate " +
            "AND l.endDate >= :startDate")
    boolean hasConflictingLeave(@Param("userId") String userId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    List<LeaveRequest> findByUserIdAndLeaveTypeAndStatus(String userId, LeaveType leaveType, LeaveStatus status);



}
