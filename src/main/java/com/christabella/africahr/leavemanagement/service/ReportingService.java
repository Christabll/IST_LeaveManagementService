package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.dto.ReportDto;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final LeaveRequestRepository leaveRequestRepository;

    public List<ReportDto> generateAllLeaveReports() {
        List<LeaveRequest> allRequests = leaveRequestRepository.findAll();

        if (allRequests.isEmpty()) {
            throw new ResourceNotFoundException("No leave requests found to generate reports.");
        }

        return allRequests.stream().map(request -> ReportDto.builder()
                .employeeName("User #" + request.getUserId())
                .leaveType(request.getLeaveType().getName())
                .totalDaysTaken(
                        ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1
                )
                .build()).collect(Collectors.toList());
    }

}
