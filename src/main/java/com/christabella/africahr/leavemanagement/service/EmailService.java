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
        if (to == null || to.isBlank()) {
            log.error("Cannot send email: recipient email is null or blank");
            return;
        }

        try {
            log.info("Preparing to send email to: {} with subject: {}", to, subject);
            log.debug("Email template: {}, model: {}", templateName, model);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            Context context = new Context();
            context.setVariables(model);
            String htmlContent = templateEngine.process(templateName, context);
            log.debug("Generated HTML content length: {}", htmlContent.length());

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.info("Attempting to send email to: {}", to);
            mailSender.send(message);
            log.info("Successfully sent email to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email to " + to, e);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Unexpected error while sending email to " + to, e);
        }
    }

    public void sendLeaveSubmissionEmail(LeaveRequest request) {
        if (request == null || request.getUserId() == null) {
            log.error("Cannot send leave submission email: request or userId is null");
            return;
        }

        String userId = request.getUserId();
        log.info("Attempting to send leave submission email for user: {}", userId);
        
        String email = userServiceClient.getUserEmail(userId);
        String name = userServiceClient.getUserFullName(userId);

        if (email == null || email.isBlank()) {
            log.error("Cannot send leave submission email: user email is null or blank for userId: {}", userId);
            return;
        }

        log.info("Sending leave submission email to user: {} ({})", name, email);
        Map<String, Object> model = Map.of(
                "name", name != null ? name : "",
                "startDate", request.getStartDate(),
                "endDate", request.getEndDate(),
                "status", "PENDING"
        );

        sendHtmlEmail(email, "Leave Request Submitted", "leave-notification", model);
    }
}