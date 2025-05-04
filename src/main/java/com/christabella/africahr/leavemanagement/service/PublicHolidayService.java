package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.dto.PublicHolidayDto;
import com.christabella.africahr.leavemanagement.repository.PublicHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicHolidayService {

    private final PublicHolidayRepository publicHolidayRepository;

    public List<PublicHolidayDto> getUpcomingHolidays() {
        return publicHolidayRepository.findByDateAfterOrderByDateAsc(LocalDate.now())
                .stream()
                .map(h -> PublicHolidayDto.builder()
                        .date(h.getDate())
                        .name(h.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
