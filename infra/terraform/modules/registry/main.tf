# Registry module — ECR container repositories
# Floci emulates ECR locally.

resource "aws_ecr_repository" "payment_service" {
  name                 = "payflow-${var.environment}/payment-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "payflow-${var.environment}/payment-service"
    Environment = var.environment
  }
}

resource "aws_ecr_repository" "merchant_service" {
  name                 = "payflow-${var.environment}/merchant-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "payflow-${var.environment}/merchant-service"
    Environment = var.environment
  }
}

resource "aws_ecr_repository" "webhook_service" {
  name                 = "payflow-${var.environment}/webhook-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "payflow-${var.environment}/webhook-service"
    Environment = var.environment
  }
}

resource "aws_ecr_repository" "notification_service" {
  name                 = "payflow-${var.environment}/notification-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "payflow-${var.environment}/notification-service"
    Environment = var.environment
  }
}

resource "aws_ecr_repository" "frontend" {
  name                 = "payflow-${var.environment}/frontend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "payflow-${var.environment}/frontend"
    Environment = var.environment
  }
}
