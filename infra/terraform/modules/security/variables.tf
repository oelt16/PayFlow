# Security module variables

variable "environment" {
  description = "Deployment environment (local, dev, staging, prod)"
  type        = string
}

variable "db_password" {
  description = "Master password for RDS PostgreSQL instances"
  type        = string
  sensitive   = true
}
