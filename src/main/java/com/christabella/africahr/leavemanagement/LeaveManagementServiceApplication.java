package com.christabella.africahr.leavemanagement;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeaveManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeaveManagementServiceApplication.class, args);
	}

}
