package com.hireconnect.notificationservice.listener;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hireconnect.notificationservice.dto.NotificationRequest;
import com.hireconnect.notificationservice.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationMessageListenerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void handleNotificationSendsThroughService() {
        NotificationMessageListener listener = new NotificationMessageListener(notificationService);
        NotificationRequest request = new NotificationRequest();

        listener.handleNotification(request);

        verify(notificationService).sendNotification(request);
    }
}
