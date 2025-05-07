package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.dto.ReportDto;
import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import com.christabella.africahr.leavemanagement.exception.ResourceNotFoundException;
import com.christabella.africahr.leavemanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final LeaveRequestRepository leaveRequestRepository;
    private static final String UPLOAD_DIR = "uploads/documents/";


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


    public ByteArrayInputStream exportReportsToCSV() {
        List<ReportDto> reports = generateAllLeaveReports();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                     CSVFormat.DEFAULT.withHeader("Employee", "Leave Type", "Total Days Taken"))) {

            for (ReportDto report : reports) {
                printer.printRecord(
                        report.getEmployeeName(),
                        report.getLeaveType(),
                        report.getTotalDaysTaken()
                );
            }

            printer.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }

}
