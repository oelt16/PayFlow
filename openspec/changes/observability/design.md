# Design: Phase 14 — Observability

> **Reconstructed from**: Engram observations #33, #34

## Technical Approach

Add Micrometer metrics + Spring Boot Actuator to all services, Prometheus + Grafana to Docker Compose, and optional Zipkin for distributed tracing.

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Metrics framework | Micrometer | Native Spring Boot support, Prometheus export built-in |
| Tracing | Micrometer Tracing + Zipkin | Matches actuator ecosystem, simple setup |
| Grafana provisioning | Dashboard JSON + datasource YAML | Reproducible, version-controlled |
| Alert thresholds | Outbox pending > 100 for 5 min | Most critical operational metric |

## File Changes

| File | Action | Service |
|------|--------|---------|
| `pom.xml` | Modify | All: add actuator + micrometer + tracing deps |
| `application.yml` | Modify | All: management endpoints config |
| `PaymentServiceMetrics.java` | Create | payment-service |
| `WebhookServiceMetrics.java` | Create | webhook-service |
| `OutboxMetrics.java` | Create | All services |
| `docker-compose.yml` | Modify | Root: add prometheus + grafana + zipkin |
| `prometheus.yml` | Create | Infra |
| `grafana/dashboards/payflow.json` | Create | Infra |

## Data Flow

```
Payment captured → PaymentApplicationService
    → MeterRegistry.counter("payflow.payment.captured").increment()
    → Timer.Sample.stop(timer)

Outbox relay → Gauge: outbox.pending = countByPublished(false)

Prometheus scrapes /actuator/prometheus every 15s
    → Grafana dashboard queries Prometheus
    → Panels render counter rate, histograms, gauges
```
