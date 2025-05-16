package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.dto.LeaveReportDto;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserServiceClient userServiceClient;
    private static final String UPLOAD_DIR = "uploads/documents/";

    public List<LeaveReportDto> getLeaveReports(String type, String department, String status, String start, String end) {
        List<LeaveRequest> requests = leaveRequestRepository.findAll();
        // Filter by leave type
        if (type != null && !type.isBlank()) {
            requests = requests.stream()
                    .filter(r -> r.getLeaveType().getName().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        // Filter by status
        if (status != null && !status.isBlank()) {
            requests = requests.stream()
                    .filter(r -> r.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }
        // Filter by date range
        if (start != null && end != null) {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            requests = requests.stream()
                    .filter(r -> !r.getStartDate().isAfter(endDate) && !r.getEndDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }
        // Filter by department
        if (department != null && !department.isBlank()) {
            requests = requests.stream()
                    .filter(r -> department.equalsIgnoreCase(userServiceClient.getUserDepartment(r.getUserId())))
                    .collect(Collectors.toList());
        }
        return requests.stream().map(request -> {
            String employeeName = userServiceClient.getUserFullName(request.getUserId());
            String employeeEmail = userServiceClient.getUserEmail(request.getUserId());
            String dept = userServiceClient.getUserDepartment(request.getUserId());
            String role = userServiceClient.getUserRole(request.getUserId());
            String approverName = null;
            String approverComment = request.getApproverComment();
            return LeaveReportDto.builder()
                    .employeeName(employeeName)
                    .employeeEmail(employeeEmail)
                    .department(dept)
                    .role(role)
                    .leaveType(request.getLeaveType().getName())
                    .status(request.getStatus().name())
                    .startDate(request.getStartDate().toString())
                    .endDate(request.getEndDate().toString())
                    .days(java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1)
                    .reason(request.getReason())
                    .documentUrl(request.getDocumentUrl())
                    .approverName(approverName)
                    .approverComment(approverComment)
                    .build();
        }).collect(Collectors.toList());
    }

}
