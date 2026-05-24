# Proposal: Phase 14 — Observability (Metrics + Tracing)

> **Reconstructed from**: Engram observations #32, #33, #34

## Intent

Add production-grade observability to PayFlow — Prometheus metrics, Grafana dashboards, and optional distributed tracing. Transforms interview answers from theoretical to demonstrable.

## Scope

- All 4 backend services: payment-service, merchant-service, webhook-service, notification-service
- Prometheus + Grafana in Docker Compose
- Optional: Zipkin distributed tracing

## Approach

| Component | Implementation |
|-----------|---------------|
| Custom metrics | Micrometer counters, timers, gauges per service |
| Actuator | `/actuator/prometheus` endpoint on all services |
| Monitoring stack | Prometheus (scrape) + Grafana (dashboard) |
| Tracing | Micrometer Tracing + Zipkin exporter |
