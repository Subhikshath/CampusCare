package com.CampusCare.notification;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class NotificationResponse {
    Long id;
    String message;
    boolean read;
    Long complaintId;
    LocalDateTime createdAt;
}