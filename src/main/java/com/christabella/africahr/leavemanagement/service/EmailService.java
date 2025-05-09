package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.entity.LeaveRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserServiceClient userServiceClient;


    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> model) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            Context context = new Context();
            context.setVariables(model);
            String htmlContent = templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendLeaveSubmissionEmail(LeaveRequest request) {
        String userId = request.getUserId();
        String email = userServiceClient.getUserEmail(userId);
        String name = userServiceClient.getUserFullName(userId);

        Map<String, Object> model = Map.of(
                "name", name,
                "startDate", request.getStartDate(),
                "endDate", request.getEndDate(),
                "status", "PENDING"
        );

        sendHtmlEmail(email, "Leave Request Submitted", "leave-notification", model);
    }

}