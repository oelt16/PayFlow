# Data module — RDS PostgreSQL instances (Floci-managed PG containers)
# Floci's RDS proxy manages real PostgreSQL containers behind the RDS API.
# Each instance maps to a unique port in the 7001-7003 range.

resource "aws_db_instance" "payment" {
  identifier     = "payflow-${var.environment}-payment"
  engine         = "postgres"
  engine_version = "16"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  storage_type          = "gp2"
  storage_encrypted     = true
  kms_key_id            = var.kms_key_arn

  db_name  = "payflow"
  username = "payflow"
  password = var.db_password
  port     = 7001

  # Floci manages the underlying PG container lifecycle
  skip_final_snapshot = true
  publicly_accessible = false

  tags = {
    Name        = "payflow-${var.environment}-payment-db"
    Environment = var.environment
    Service     = "payment-service"
  }
}

resource "aws_db_instance" "merchant" {
  identifier     = "payflow-${var.environment}-merchant"
  engine         = "postgres"
  engine_version = "16"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  storage_type          = "gp2"
  storage_encrypted     = true
  kms_key_id            = var.kms_key_arn

  db_name  = "payflow"
  username = "payflow"
  password = var.db_password
  port     = 7002

  skip_final_snapshot = true
  publicly_accessible = false

  tags = {
    Name        = "payflow-${var.environment}-merchant-db"
    Environment = var.environment
    Service     = "merchant-service"
  }
}

resource "aws_db_instance" "webhook" {
  identifier     = "payflow-${var.environment}-webhook"
  engine         = "postgres"
  engine_version = "16"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  storage_type          = "gp2"
  storage_encrypted     = true
  kms_key_id            = var.kms_key_arn

  db_name  = "payflow"
  username = "payflow"
  password = var.db_password
  port     = 7003

  skip_final_snapshot = true
  publicly_accessible = false

  tags = {
    Name        = "payflow-${var.environment}-webhook-db"
    Environment = var.environment
    Service     = "webhook-service"
  }
}
