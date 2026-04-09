# Notification service

Consumes `payments.events` and forwards each envelope to webhook-service via `POST {payflow.webhook-dispatch.base-url}/internal/webhooks/dispatch` (configurable; can be disabled with `payflow.webhook-dispatch.enabled=false`).

Default port: **8084**.
