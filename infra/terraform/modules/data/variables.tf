# Data module variables

variable "environment" {
  description = "Deployment environment (local, dev, staging, prod)"
  type        = string
}

variable "db_password" {
  description = "Master password for RDS instances"
  type        = string
  sensitive   = true
}

variable "kms_key_arn" {
  description = "ARN of the KMS key for storage encryption"
  type        = string
  default     = null  # Floci may not enforce encryption locally
}
