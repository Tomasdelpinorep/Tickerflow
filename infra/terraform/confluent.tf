provider "confluent" {
  cloud_api_key = var.confluent_cloud_api_key
  cloud_api_secret = var.confluent_cloud_api_secret
}

resource "confluent_environment" "development" {
  display_name = "dev"
}

# Create basic-tier cluster
resource "confluent_kafka_cluster" "basic" {
  availability = "SINGLE_ZONE"
  cloud        = "AWS"
  display_name = "tickerflow-kafka"
  region       = var.aws_region

  basic {}

  environment {
    id = confluent_environment.development.id
  }
}

# API keys belong to a principal, in this case, tickerflow's spring services, aka the tickerflow app
resource "confluent_service_account" "tickerflow" {
  display_name = "tickerflow-app"
  description  = "Used by TickerFlow services to auth to Kafka"
}

# Create an API key for our basic cluster
resource "confluent_api_key" "cluster_api_key" {
  display_name = "app-manager-kafka-api-key"
  owner {
    api_version = confluent_service_account.tickerflow.api_version
    id          = confluent_service_account.tickerflow.id
    kind        = confluent_service_account.tickerflow.kind
  }
  managed_resource {
    api_version = confluent_kafka_cluster.basic.api_version
    id          = confluent_kafka_cluster.basic.id
    kind        = confluent_kafka_cluster.basic.kind

    environment {
      id = confluent_environment.development.id
    }
  }
}