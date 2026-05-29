# Registry module outputs

output "registry_url" {
  description = "URL of the ECR registry (same for all repos under Floci)"
  value       = aws_ecr_repository.payment_service.repository_url
}

output "payment_service_repo_url" {
  description = "URL of the payment-service ECR repository"
  value       = aws_ecr_repository.payment_service.repository_url
}

output "merchant_service_repo_url" {
  description = "URL of the merchant-service ECR repository"
  value       = aws_ecr_repository.merchant_service.repository_url
}

output "webhook_service_repo_url" {
  description = "URL of the webhook-service ECR repository"
  value       = aws_ecr_repository.webhook_service.repository_url
}

output "notification_service_repo_url" {
  description = "URL of the notification-service ECR repository"
  value       = aws_ecr_repository.notification_service.repository_url
}

output "frontend_repo_url" {
  description = "URL of the frontend ECR repository"
  value       = aws_ecr_repository.frontend.repository_url
}
