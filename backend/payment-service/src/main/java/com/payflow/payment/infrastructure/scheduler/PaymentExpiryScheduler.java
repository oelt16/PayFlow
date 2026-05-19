package com.payflow.payment.infrastructure.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.payflow.payment.application.port.DomainEventOutbox;
import com.payflow.payment.application.port.PaymentRepository;
import com.payflow.payment.domain.Payment;

/**
 * Scheduler for expiring pending payments that have passed their expiration time.
 * 
 * Runs every 5 minutes.
 * Query: status=PENDING AND createdAt < (now - 1h) AND expiresAt <= now
 * 
 * Transaction Strategy: Each payment.expire() + update runs in its own transaction 
 * via auto-commit. This ensures one failure doesn't rollback the entire batch.
 */
@Component
public class PaymentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryScheduler.class);
    
    private static final Duration PENDING_THRESHOLD = Duration.ofHours(1);
    private static final int BATCH_SIZE = 100;

    private final PaymentRepository paymentRepository;
    private final DomainEventOutbox outboxAppender;
    private final Clock clock;

    public PaymentExpiryScheduler(
            PaymentRepository paymentRepository,
            DomainEventOutbox outboxAppender,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.outboxAppender = outboxAppender;
        this.clock = clock;
    }

    /**
     * Main scheduled method - runs every 5 minutes.
     * No @Transactional - each payment update runs in its own transaction.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void runExpiryJob() {
        log.info("Starting payment expiry job");
        Instant now = clock.instant();
        Instant createdBefore = now.minus(PENDING_THRESHOLD);
        
        List<Payment> expiredPayments = paymentRepository.findPendingOlderThan(createdBefore, now, BATCH_SIZE);
        
        log.info("Found {} pending payments eligible for expiry", expiredPayments.size());
        
        int processed = 0;
        for (Payment payment : expiredPayments) {
            try {
                // Each payment expire() and update runs in its own "transaction" 
                // through auto-commit (no @Transactional on this method)
                Instant currentTime = clock.instant();
                payment.expire(currentTime);
                paymentRepository.update(payment);
                
                // Append domain events to outbox
                outboxAppender.append(payment.id().value(), payment.pullDomainEvents());
                
                processed++;
                log.debug("Expired payment: {}", payment.id().value());
            } catch (Exception e) {
                log.error("Failed to expire payment {}: {}", payment.id().value(), e.getMessage());
                // Continue with next payment - one failure shouldn't stop the batch
            }
        }
        
        log.info("Payment expiry job completed. Processed: {}/{}", processed, expiredPayments.size());
    }
}