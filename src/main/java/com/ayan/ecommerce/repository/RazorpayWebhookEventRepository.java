package com.ayan.ecommerce.repository;

import com.ayan.ecommerce.entity.RazorpayWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RazorpayWebhookEventRepository
        extends JpaRepository<RazorpayWebhookEvent, Long> {

    Optional<RazorpayWebhookEvent> findByEventId(
            String eventId
    );
}