package com.hireconnect.notificationservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.hireconnect.notificationservice.dto.NotificationRequest;
import com.hireconnect.notificationservice.service.NotificationService;

@Component
public class NotificationMessageListener {

    private final NotificationService notificationService;

    public NotificationMessageListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${app.rabbitmq.notification-queue}")
    public void handleNotification(NotificationRequest request) {
        notificationService.sendNotification(request);
    }
}
