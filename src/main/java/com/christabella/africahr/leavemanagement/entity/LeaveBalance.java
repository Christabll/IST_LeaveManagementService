package com.christabella.africahr.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @ManyToOne
    private LeaveType leaveType;

    private double balance;

    private double carryOver;

    private boolean manuallyAdjusted;
}
