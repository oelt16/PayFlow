package com.payflow.payment.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.payflow.payment.infrastructure.persistence.jpa.IdempotencyKeySpringDataRepository;

/**
 * Scheduler for purging expired idempotency keys.
 * Runs daily at 2am UTC to clean up old idempotency records.
 */
@Component
public class IdempotencyPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyPurgeScheduler.class);

    private final IdempotencyKeySpringDataRepository repository;

    public IdempotencyPurgeScheduler(IdempotencyKeySpringDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Purges expired idempotency keys.
     * Runs daily at 2am UTC.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void purgeExpiredKeys() {
        log.info("Starting idempotency key purge");
        long deleted = repository.deleteByExpiresAtBefore(java.time.Instant.now());
        log.info("Purged {} expired idempotency keys", deleted);
    }
}