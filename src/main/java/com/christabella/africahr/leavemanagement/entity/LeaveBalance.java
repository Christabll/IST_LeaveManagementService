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

    private String userEmail;
   
    @ManyToOne
    private LeaveType leaveType;

    private double defaultBalance;

    private double usedLeave;
    
    private String remainingLeave;

    private double carryOver;
    
    private int year;

    private boolean manuallyAdjusted;
}
