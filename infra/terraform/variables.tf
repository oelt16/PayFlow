# Root module input variables

variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment (local, dev, staging, prod)"
  type        = string
  default     = "local"
}

variable "db_password" {
  description = "Master password for RDS PostgreSQL instances"
  type        = string
  sensitive   = true
}
