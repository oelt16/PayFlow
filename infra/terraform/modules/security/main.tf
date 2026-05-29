# Security module — KMS key, Secrets Manager secrets, IAM policies
# Creates a single KMS key used to encrypt all secrets in this module.

# ---------------------------------------------------------------------------
# KMS key
# ---------------------------------------------------------------------------
resource "aws_kms_key" "main" {
  description             = "PayFlow ${var.environment} — secret encryption key"
  deletion_window_in_days = 7
  enable_key_rotation     = true
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "EnableRootAccess"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::000000000000:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
    ]
  })
}

resource "aws_kms_alias" "main" {
  name          = "alias/payflow-${var.environment}"
  target_key_id = aws_kms_key.main.key_id
}

# ---------------------------------------------------------------------------
# Secrets Manager secrets
# ---------------------------------------------------------------------------

# Payment service DB secret
resource "aws_secretsmanager_secret" "payment_db" {
  name                    = "/payflow/${var.environment}/payment-service/db"
  kms_key_id             = aws_kms_key.main.arn
  recovery_window_in_days = 0  # 0 for local/development — immediate deletion
}

resource "aws_secretsmanager_secret_version" "payment_db" {
  secret_id = aws_secretsmanager_secret.payment_db.id
  secret_string = jsonencode({
    url      = "jdbc:postgresql://localhost:7001/payflow"
    username = "payflow"
    password = var.db_password
  })
}

# Merchant service DB secret
resource "aws_secretsmanager_secret" "merchant_db" {
  name                    = "/payflow/${var.environment}/merchant-service/db"
  kms_key_id             = aws_kms_key.main.arn
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "merchant_db" {
  secret_id = aws_secretsmanager_secret.merchant_db.id
  secret_string = jsonencode({
    url      = "jdbc:postgresql://localhost:7002/payflow"
    username = "payflow"
    password = var.db_password
  })
}

# Webhook service DB secret
resource "aws_secretsmanager_secret" "webhook_db" {
  name                    = "/payflow/${var.environment}/webhook-service/db"
  kms_key_id             = aws_kms_key.main.arn
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "webhook_db" {
  secret_id = aws_secretsmanager_secret.webhook_db.id
  secret_string = jsonencode({
    url      = "jdbc:postgresql://localhost:7003/payflow?currentSchema=webhooks"
    username = "payflow"
    password = var.db_password
  })
}

# Kafka bootstrap secret
resource "aws_secretsmanager_secret" "kafka" {
  name                    = "/payflow/${var.environment}/kafka"
  kms_key_id             = aws_kms_key.main.arn
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "kafka" {
  secret_id = aws_secretsmanager_secret.kafka.id
  secret_string = jsonencode({
    bootstrap_servers = "localhost:9092"
  })
}

# ---------------------------------------------------------------------------
# IAM policies — Floci emulates the IAM API for local development
# ---------------------------------------------------------------------------

# Payment service policy: read own secret + KMS crypto operations + CloudWatch logs
resource "aws_iam_policy" "payment_service" {
  name        = "payflow-${var.environment}-payment-service"
  description = "IAM policy for PayFlow payment-service (${var.environment})"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadOwnSecret"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = aws_secretsmanager_secret.payment_db.arn
      },
      {
        Sid    = "KmsCryptoOps"
        Effect = "Allow"
        Action = [
          "kms:GenerateDataKey",
          "kms:Decrypt",
        ]
        Resource = aws_kms_key.main.arn
      },
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams",
        ]
        Resource = "*"
      },
    ]
  })
}

# Merchant service policy: read own secret only
resource "aws_iam_policy" "merchant_service" {
  name        = "payflow-${var.environment}-merchant-service"
  description = "IAM policy for PayFlow merchant-service (${var.environment})"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadOwnSecret"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = aws_secretsmanager_secret.merchant_db.arn
      },
    ]
  })
}

# Webhook service policy: read own secret only
resource "aws_iam_policy" "webhook_service" {
  name        = "payflow-${var.environment}-webhook-service"
  description = "IAM policy for PayFlow webhook-service (${var.environment})"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadOwnSecret"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = aws_secretsmanager_secret.webhook_db.arn
      },
    ]
  })
}

# Notification service policy: read kafka secret + CloudWatch logs
resource "aws_iam_policy" "notification_service" {
  name        = "payflow-${var.environment}-notification-service"
  description = "IAM policy for PayFlow notification-service (${var.environment})"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadKafkaSecret"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = aws_secretsmanager_secret.kafka.arn
      },
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams",
        ]
        Resource = "*"
      },
    ]
  })
}
