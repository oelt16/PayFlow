# Security module outputs

output "kms_key_arn" {
  description = "ARN of the KMS key"
  value       = aws_kms_key.main.arn
}

output "payment_db_secret_arn" {
  description = "ARN of the payment-service database secret"
  value       = aws_secretsmanager_secret.payment_db.arn
}

output "merchant_db_secret_arn" {
  description = "ARN of the merchant-service database secret"
  value       = aws_secretsmanager_secret.merchant_db.arn
}

output "webhook_db_secret_arn" {
  description = "ARN of the webhook-service database secret"
  value       = aws_secretsmanager_secret.webhook_db.arn
}

output "kafka_secret_arn" {
  description = "ARN of the Kafka bootstrap secret"
  value       = aws_secretsmanager_secret.kafka.arn
}
