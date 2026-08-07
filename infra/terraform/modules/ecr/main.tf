locals {
  services = [
    "ingestion-service",
    "candle-aggregator",
    "moving-avg-processor",
    "trading-engine",
    "notification-service",
  ]
}

resource "aws_ecr_repository" "services" {
  for_each = toset(local.services)

  name                 = "tickerflow/${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Environment = var.environment }
}