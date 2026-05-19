# Phase 14: Observability — Metrics + Tracing

## Overview

This document explains the observability infrastructure added in Phase 14: Metrics and Tracing for PayFlow.

## What Was Implemented

### 1. Dependencies Added

Added to all 4 backend services (payment-service, merchant-service, webhook-service, notification-service):

```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Distributed Tracing (Optional) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### 2. Application Configuration

Added to each service's `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
```

### 3. Custom Metrics (payment-service)

Created `PaymentServiceMetrics` class with:
- **Counters:**
  - `payflow.payment.created` (tag: currency)
  - `payflow.payment.captured`
  - `payflow.payment.cancelled`
  - `payflow.payment.refunded`
  - `payflow.payment.expired`
  - `payflow.payment.failed` (tag: reason)

- **Distribution Summaries:**
  - `payflow.payment.amount` (tag: currency, baseUnit: cents)

- **Timers:**
  - `payflow.payment.capture.latency`

### 4. Outbox Metrics

Created `OutboxMetrics` class with:
- **Gauge:** `payflow.outbox.pending` — tracks unpublished outbox events per service

### 5. Infrastructure (Docker Compose)

Added to `infra/docker-compose.yml`:
- **Prometheus** (port 9090) — metrics collection
- **Grafana** (port 3001) — visualization
- **Zipkin** (port 9411) — distributed tracing

### 6. Prometheus Configuration

Created `infra/prometheus.yml` with scrape targets for all 4 services.

### 7. Grafana Dashboard

Created `infra/grafana/dashboards/payflow.json` with panels for:
- Payments created per minute (by currency)
- Payment success rate (captured / created %)
- Captured volume in USD/EUR/GBP
- P50/P95/P99 capture latency
- Webhook delivery success rate
- Outbox pending depth (alert threshold: >100)

---

## Why This Matters

### Interview Talking Points

1. **"The outbox pending gauge is the single most important operational metric. If it grows indefinitely, events stop flowing. I pre-configured a Grafana alert at >100 pending for 5 minutes, which gives on-call 5 minutes to react before any SLA breach."**

2. **"I chose Micrometer because it's implementation-agnostic — we can swap Prometheus for DataDog or CloudWatch without changing application code."**

3. **"The payment capture latency timer uses histogram_quantile in Prometheus to get P50/P95/P99 — this helps identify tail latency issues that averages would miss."**

4. **"Tracing with Zipkin spans from payment-service → Kafka → notification-service → webhook-service, giving end-to-end visibility into the async flow."**

---

## How to Use

### 1. Start the Stack

```bash
cd infra
docker compose up --build
```

Services available:
- Payment Service: http://localhost:8081
- Merchant Service: http://localhost:8082
- Webhook Service: http://localhost:8083
- Notification Service: http://localhost:8084 (internal)
- Frontend: http://localhost:3000
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)
- Zipkin: http://localhost:9411

### 2. Access Metrics

Prometheus endpoint on each service:
```
http://localhost:8081/actuator/prometheus
http://localhost:8082/actuator/prometheus
http://localhost:8083/actuator/prometheus
http://localhost:8084/actuator/prometheus
```

### 3. View Grafana Dashboard

1. Open http://localhost:3001
2. Login: admin / admin
3. Navigate to Dashboards → PayFlow Observability Dashboard

### 4. Query Prometheus Directly

Example queries:

```promql
# Payments created per minute by currency
sum(rate(payflow_payment_created_total[1m])) by (currency)

# Payment success rate (5m window)
sum(rate(payflow_payment_captured_total[5m])) / sum(rate(payflow_payment_created_total[5m])) * 100

# Capture latency percentiles
histogram_quantile(0.95, sum(rate(payflow_payment_capture_latency_seconds_bucket[5m])) by (le))

# Outbox pending events per service
payflow_outbox_pending
```

---

## Testing

### Unit Tests

The `PaymentApplicationServiceTest` has been updated to include the `PaymentServiceMetrics` mock. Run with:

```bash
cd backend
./mvnw test -pl payment-service -Dtest=PaymentApplicationServiceTest
```

### Integration Tests

Integration tests require Docker (for Testcontainers). Run with:

```bash
cd backend
./mvnw verify
```

Note: Integration tests will fail if Docker is not available. This is expected in environments without Docker.

---

## Debugging

### Common Issues

1. **Metrics not appearing in Prometheus**
   - Verify actuator endpoints are exposed: `curl http://localhost:8081/actuator/prometheus`
   - Check Prometheus target status at http://localhost:9090/targets
   - Verify `management.endpoints.web.exposure.include` includes `prometheus`

2. **Outbox pending gauge shows -1 or NaN**
   - The gauge uses `countByPublishedFalse()` from the repository
   - Check if the outbox table exists and has data
   - Verify the repository method returns a valid count

3. **Grafana dashboard shows "No data"**
   - Verify Prometheus is scraping the targets
   - Check the query in the dashboard panel
   - Ensure metrics have been generated (make some API calls first)

4. **Zipkin shows no traces**
   - Verify `management.tracing.sampling.probability` is set to 1.0 for full sampling
   - Check Zipkin is accessible at http://localhost:9411
   - Make HTTP requests to generate traces

### Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    com.payflow.payment.infrastructure.metrics: DEBUG
    io.micrometer: DEBUG
```

---

## Alerting

The Grafana dashboard includes an alert for outbox pending events:

- **Alert:** Outbox Pending > 100 for 5 minutes
- **Severity:** Warning
- **Action:** Check Kafka connectivity and outbox relay health

To configure email/PagerDuty alerts in Grafana:
1. Go to Alerting → Notification channels
2. Add your notification channel
3. Edit the dashboard panel and add an alert rule

---

## Future Enhancements

1. **Add webhook delivery metrics** — track attempted/succeeded/failed webhook deliveries
2. **Add Kafka consumer lag metrics** — monitor consumer offset lag per partition
3. **Add SLI/SLO dashboards** — define availability and latency targets
4. **Integrate with cloud monitoring** — swap Prometheus for CloudWatch, DataDog, or GCP Monitoring

---

## Files Changed

- `backend/*/pom.xml` — Added actuator, micrometer, brave dependencies
- `backend/*/src/main/resources/application.yml` — Added management config
- `backend/payment-service/src/main/java/.../metrics/PaymentServiceMetrics.java` — New
- `backend/payment-service/src/main/java/.../metrics/OutboxMetrics.java` — New
- `backend/payment-service/src/main/java/.../application/PaymentApplicationService.java` — Updated to record metrics
- `backend/payment-service/src/main/java/.../scheduler/PaymentExpiryScheduler.java` — Updated to record metrics
- `backend/payment-service/src/main/java/.../persistence/jpa/OutboxEventSpringDataRepository.java` — Added count method
- `backend/payment-service/src/test/java/.../PaymentApplicationServiceTest.java` — Updated for metrics mock
- `infra/docker-compose.yml` — Added Prometheus, Grafana, Zipkin
- `infra/prometheus.yml` — New
- `infra/grafana/provisioning/datasources/datasources.yml` — New
- `infra/grafana/dashboards/payflow.json` — New

---

*Phase 14 — Observability: Metrics + Tracing*
*PayFlow Payment Processing Platform*