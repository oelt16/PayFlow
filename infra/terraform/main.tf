# PayFlow local infrastructure — AWS emulated by Floci
# Apply:   terraform apply -var="environment=local"
# Destroy: terraform destroy -var="environment=local"

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  # Floci AWS emulator endpoint
  endpoints {
    apigatewayv2       = "http://localhost:4566"
    cloudwatch         = "http://localhost:4566"
    ecr                = "http://localhost:4566"
    iam                = "http://localhost:4566"
    kafka              = "http://localhost:4566"  # MSK control plane
    kms                = "http://localhost:4566"
    rds                = "http://localhost:4566"
    secretsmanager     = "http://localhost:4566"
  }

  skip_credentials_validation = true
  skip_requesting_account_id  = true
  skip_metadata_api_check     = true
  s3_use_path_style           = true

  # Floci static credentials
  access_key = "test"
  secret_key = "test"
}

# ---------------------------------------------------------------------------
# Module: Security — KMS key + Secrets Manager secrets + IAM policies
# ---------------------------------------------------------------------------
module "security" {
  source      = "./modules/security"
  environment = var.environment
  db_password = var.db_password
}

# ---------------------------------------------------------------------------
# Module: Data — RDS PostgreSQL instances (Floci-managed PG containers)
# ---------------------------------------------------------------------------
module "data" {
  source      = "./modules/data"
  environment = var.environment
  db_password = var.db_password
  kms_key_arn = module.security.kms_key_arn
}

# ---------------------------------------------------------------------------
# Module: Messaging — MSK / Redpanda Kafka cluster
# ---------------------------------------------------------------------------
module "messaging" {
  source      = "./modules/messaging"
  environment = var.environment
}

# ---------------------------------------------------------------------------
# Module: Registry — ECR container repositories
# ---------------------------------------------------------------------------
module "registry" {
  source      = "./modules/registry"
  environment = var.environment
}
