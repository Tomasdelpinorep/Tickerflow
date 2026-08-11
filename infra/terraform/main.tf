terraform {
  required_version = ">= 1.7"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }

    confluent = {
      source  = "confluentinc/confluent"
      version = "2.63.0"
    }
  }
}