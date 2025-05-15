package com.christabella.africahr.leavemanagement.repository;

import com.christabella.africahr.leavemanagement.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    List<PublicHoliday> findByDateAfterOrderByDateAsc(LocalDate today);
    boolean existsByDate(LocalDate date);
    List<PublicHoliday> findByDateBetween(LocalDate start, LocalDate end);

}
