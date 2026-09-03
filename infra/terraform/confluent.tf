provider "confluent" {
  cloud_api_key = var.confluent_cloud_api_key
  cloud_api_secret = var.confluent_cloud_api_secret
}

resource "confluent_environment" "development" {
  display_name = "dev"

  stream_governance {
    package = "ESSENTIALS"
  }
}

data "confluent_schema_registry_cluster" "schema_registry_cluster" {
  environment {
    id = confluent_environment.development.id
  }
  depends_on = [
    confluent_environment.development
  ]
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

# Authorizes the tickerflow-app service account to actually produce/consume on the Kafka
# cluster and read/write schemas on the Schema Registry within this environment. Without this,
# the API keys above authenticate fine but every Kafka/SR operation is rejected with
# ClusterAuthorizationException — API keys are identity, role bindings are authorization.
# EnvironmentAdmin scoped to the whole environment covers both Kafka and Schema Registry in one
# binding; per-topic DeveloperWrite/DeveloperRead bindings would be the minimal-privilege choice
# but aren't worth the extra resources for a single-environment portfolio project.
resource "confluent_role_binding" "tickerflow_env_admin" {
  principal   = "User:${confluent_service_account.tickerflow.id}"
  role_name   = "EnvironmentAdmin"
  crn_pattern = confluent_environment.development.resource_name
}

# Topics as code — created here instead of manually, so a destroy/recreate of the environment
# (e.g. overnight cost teardown) doesn't silently lose them. Confluent Cloud Basic clusters don't
# auto-create topics on first produce, and none of the services declare a Spring NewTopic bean.
locals {
  topics = {
    raw-ticks    = 3 # matches candle-aggregator's 3 Kafka Streams partitions
    candles      = 3 # matches moving-avg-processor's 3 Kafka Streams partitions
    signals      = 3
    trade-events = 3
  }
}

resource "confluent_kafka_topic" "topics" {
  for_each = local.topics

  kafka_cluster {
    id = confluent_kafka_cluster.basic.id
  }

  topic_name       = each.key
  partitions_count = each.value
  rest_endpoint    = confluent_kafka_cluster.basic.rest_endpoint

  credentials {
    key    = confluent_api_key.cluster_api_key.id
    secret = confluent_api_key.cluster_api_key.secret
  }
}

# Create an API key for our schema registry
resource "confluent_api_key" "schema_registry_api_key" {
  display_name = "schema-registry-api-key"
  owner {
    api_version = confluent_service_account.tickerflow.api_version
    id          = confluent_service_account.tickerflow.id
    kind        = confluent_service_account.tickerflow.kind
  }
  managed_resource {
    api_version = data.confluent_schema_registry_cluster.schema_registry_cluster.api_version
    id          = data.confluent_schema_registry_cluster.schema_registry_cluster.id
    kind        = data.confluent_schema_registry_cluster.schema_registry_cluster.kind

    environment {
      id = confluent_environment.development.id
    }
  }
}