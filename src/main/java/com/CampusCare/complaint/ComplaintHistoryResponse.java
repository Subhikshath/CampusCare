package com.CampusCare.complaint;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ComplaintHistoryResponse {
    Long id;
    String action;
    ComplaintStatus oldStatus;
    ComplaintStatus newStatus;
    Long performedById;
    String performedByName;
    String description;
    LocalDateTime createdAt;
}
