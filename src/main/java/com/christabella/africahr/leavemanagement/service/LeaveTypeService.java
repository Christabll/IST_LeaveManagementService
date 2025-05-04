package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.dto.LeaveTypeDto;
import com.christabella.africahr.leavemanagement.entity.LeaveType;
import com.christabella.africahr.leavemanagement.exception.BadRequestException;
import com.christabella.africahr.leavemanagement.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveType createLeaveType(LeaveTypeDto dto) {
        if (leaveTypeRepository.findByName(dto.getName()).isPresent()) {
            throw new BadRequestException("Leave type already exists.");
        }

        LeaveType type = LeaveType.builder()
                .name(dto.getName())
                .defaultBalance(dto.getDefaultBalance())
                .build();

        return leaveTypeRepository.save(type);
    }

    public List<LeaveTypeDto> getAllLeaveTypes() {
        return leaveTypeRepository.findAll().stream()
                .map(type -> LeaveTypeDto.builder()
                        .name(type.getName())
                        .defaultBalance(type.getDefaultBalance())
                        .build())
                .collect(Collectors.toList());
    }

}
