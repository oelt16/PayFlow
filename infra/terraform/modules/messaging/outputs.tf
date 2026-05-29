# Messaging module outputs

output "bootstrap_brokers" {
  description = "Bootstrap brokers string for Kafka producers/consumers"
  value       = aws_msk_cluster.main.bootstrap_brokers
}

output "cluster_arn" {
  description = "ARN of the MSK cluster"
  value       = aws_msk_cluster.main.arn
}

output "zookeeper_connect_string" {
  description = "Zookeeper connection string for the MSK cluster"
  value       = aws_msk_cluster.main.zookeeper_connect_string
}
