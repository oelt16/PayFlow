# Specification: Phase 14 — Observability

> **Source**: Engram observation #33

## Requirements

### 14.1 Custom Micrometer Metrics

**payment-service:**
- Counter: `payflow.payment.created` (tag: currency)
- Counter: `payflow.payment.captured`
- Counter: `payflow.payment.failed` (tag: reason)
- DistributionSummary: `payflow.payment.amount` (tag: currency, baseUnit: cents)
- Timer: `payflow.payment.capture.latency`

**webhook-service:**
- Counter: `payflow.webhook.delivery.attempted`
- Counter: `payflow.webhook.delivery.succeeded`
- Counter: `payflow.webhook.delivery.failed`
- Gauge: `payflow.webhook.dlq.size`

**All services (outbox relay):**
- Gauge: `payflow.outbox.pending`

### 14.2 Actuator + Prometheus

```yaml
management:
  endpoints.web.exposure.include: health,info,prometheus
  metrics.export.prometheus.enabled: true
```

### 14.3 Docker Compose

- Prometheus (port 9090) — scrape all 4 services
- Grafana (port 3001) — pre-configured dashboard

### 14.4 Grafana Dashboard

Panels:
- Payments created/min
- Payment success rate
- Captured volume (by currency)
- P50/P95/P99 capture latency
- Webhook delivery success rate
- Outbox pending depth (alert: >100 for 5 min)

### 14.5 Distributed Tracing (Optional)

- Micrometer Tracing + Zipkin exporter
- Trace: payment-service → Kafka → notification-service → webhook-service
