# Root module output values

output "kms_key_arn" {
  description = "ARN of the KMS key used for secret encryption"
  value       = module.security.kms_key_arn
}

output "payment_db_secret_arn" {
  description = "ARN of the payment-service database secret"
  value       = module.security.payment_db_secret_arn
}

output "merchant_db_secret_arn" {
  description = "ARN of the merchant-service database secret"
  value       = module.security.merchant_db_secret_arn
}

output "webhook_db_secret_arn" {
  description = "ARN of the webhook-service database secret"
  value       = module.security.webhook_db_secret_arn
}

output "kafka_secret_arn" {
  description = "ARN of the Kafka bootstrap secret"
  value       = module.security.kafka_secret_arn
}

output "bootstrap_brokers" {
  description = "Bootstrap brokers string for the MSK/Kafka cluster"
  value       = module.messaging.bootstrap_brokers
}

output "registry_url" {
  description = "URL of the ECR registry (single placeholder — all repos share the same Floci endpoint)"
  value       = module.registry.registry_url
}
