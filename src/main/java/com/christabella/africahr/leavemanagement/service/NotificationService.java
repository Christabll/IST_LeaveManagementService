package com.christabella.africahr.leavemanagement.service;

import com.christabella.africahr.leavemanagement.entity.Notification;
import com.christabella.africahr.leavemanagement.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void notify(String userId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }


    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }


    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(n -> !n.isRead()).toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
