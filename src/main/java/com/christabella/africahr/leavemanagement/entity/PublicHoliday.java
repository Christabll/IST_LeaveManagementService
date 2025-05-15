package com.christabella.africahr.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor 
@Builder
public class PublicHoliday {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private LocalDate date;
        private String name;
        private String description;

    public PublicHoliday(Long id, LocalDate date, String name) {
        this.id = id;
        this.date = date;
        this.name = name;
    }
    
    public PublicHoliday(Long id, LocalDate date, String name, String description) {
        this.id = id;
        this.date = date;
        this.name = name;
        this.description = description;
    }
}

