package com.christabella.africahr.leavemanagement.Config;

import com.christabella.africahr.leavemanagement.entity.PublicHoliday;
import com.christabella.africahr.leavemanagement.repository.PublicHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class PublicHolidaySeeder implements CommandLineRunner {

    private final PublicHolidayRepository publicHolidayRepository;

    @Override
    public void run(String... args) {
        List<PublicHoliday> holidays = List.of(
                new PublicHoliday(null, LocalDate.of(2025, 1, 1), "New Year's Day"),
                new PublicHoliday(null, LocalDate.of(2025, 2, 1), "National Heroes Day"),
                new PublicHoliday(null, LocalDate.of(2025, 4, 7), "Genocide Memorial Day"),
                new PublicHoliday(null, LocalDate.of(2025, 5, 1), "Labour Day"),
                new PublicHoliday(null, LocalDate.of(2025, 7, 1), "Independence Day"),
                new PublicHoliday(null, LocalDate.of(2025, 7, 4), "Liberation Day"),
                new PublicHoliday(null, LocalDate.of(2025, 8, 15), "Assumption Day"),
                new PublicHoliday(null, LocalDate.of(2025, 12, 25), "Christmas Day"),
                new PublicHoliday(null, LocalDate.of(2025, 12, 26), "Boxing Day")
        );

        holidays.forEach(holiday -> {
            if (!publicHolidayRepository.existsByDate(holiday.getDate())) {
                publicHolidayRepository.save(holiday);
            }
        });
    }
}
