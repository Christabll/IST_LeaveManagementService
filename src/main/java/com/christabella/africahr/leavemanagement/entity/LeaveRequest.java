package com.christabella.africahr.leavemanagement.entity;

import com.christabella.africahr.leavemanagement.enums.LeaveStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @ManyToOne
    private LeaveType leaveType;

    private LocalDate startDate;
    private LocalDate endDate;

    private String reason;

    private String documentUrl;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;

    private String approverComment;
}
