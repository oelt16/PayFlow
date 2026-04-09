# PayFlow Webhook Service

Webhooks bounded context: register HTTPS endpoints (max 5 per merchant), HMAC-SHA256 signed POST delivery, exponential backoff retries (5 attempts), and failed deliveries published to Kafka topic `webhook.dlq`.

Internal API: `POST /internal/webhooks/dispatch` (body: `merchantId`, `eventType`, `eventPayload`) used by notification-service.

Public API: `POST/GET/DELETE /v1/webhooks`, `GET /v1/webhooks/{id}/deliveries` (API key auth, same style as payment-service).

Default port: **8083**.
