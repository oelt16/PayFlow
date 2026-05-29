# Data module outputs

output "payment_db_endpoint" {
  description = "Endpoint of the payment RDS instance"
  value       = aws_db_instance.payment.endpoint
}

output "merchant_db_endpoint" {
  description = "Endpoint of the merchant RDS instance"
  value       = aws_db_instance.merchant.endpoint
}

output "webhook_db_endpoint" {
  description = "Endpoint of the webhook RDS instance"
  value       = aws_db_instance.webhook.endpoint
}

output "payment_db_address" {
  description = "Address of the payment RDS instance"
  value       = aws_db_instance.payment.address
}

output "merchant_db_address" {
  description = "Address of the merchant RDS instance"
  value       = aws_db_instance.merchant.address
}

output "webhook_db_address" {
  description = "Address of the webhook RDS instance"
  value       = aws_db_instance.webhook.address
}
