package com.ayan.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "razorpay_webhook_events",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_razorpay_webhook_event_id",
                    columnNames = "event_id"
            )
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(
            name = "event_id",
            nullable = false,
            unique = true
    )
    private String eventId;

    @Column(name = "event_type",
            nullable = false)
    private String eventType;

    @Column(
            name = "received_at",
            nullable = false
    )
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "processing_status",
            nullable = false
    )
    private WebhookProcessingStatus processingStatus;

    @PrePersist
    protected void onCreate() {

        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }

        if (processingStatus == null) {
            processingStatus =
                    WebhookProcessingStatus.PROCESSED;
        }
    }
}
