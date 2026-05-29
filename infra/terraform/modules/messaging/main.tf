# Messaging module — MSK / Redpanda Kafka cluster
# Floci emulates MSK using a Redpanda container.

resource "aws_msk_cluster" "main" {
  cluster_name  = "payflow-${var.environment}"
  kafka_version = "3.6.1"

  number_of_broker_nodes = 1

  broker_node_group_info {
    instance_type = "kafka.t3.small"
    client_subnets = [
      "subnet-00000000000000000",  # Floci accepts any valid subnet ARN
    ]
    security_groups = []  # Floci does not enforce SG rules
  }

  # Floci emulates MSK control plane; actual Kafka traffic routes through Redpanda
  encryption_info {
    encryption_at_rest_kms_key_arn = ""  # Floci: leave empty for local
  }

  tags = {
    Name        = "payflow-${var.environment}-msk"
    Environment = var.environment
  }
}
