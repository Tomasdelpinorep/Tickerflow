variable "environment" {
  type = string
}

variable "public_subnet_id" {
  type = string
}

variable "instance_sg_id" {
  type = string
}

variable "ec2_key_name" {
  type = string
}

variable "ec2_registry_url" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "mailgun_smtp_username" {
  type      = string
  sensitive = true
}

variable "mailgun_smtp_password" {
  type      = string
  sensitive = true
}

variable "personal_email" {
  type      = string
  sensitive = true
}

variable "spring_kafka_bootstrap_servers" {
  type = string
}

variable "spring_kafka_api_key" {
  type      = string
  sensitive = true
}

variable "spring_kafka_api_secret" {
  type      = string
  sensitive = true
}

variable "spring_datasource_url" {
  type = string
}

variable "spring_datasource_username" {
  type = string
}

variable "spring_datasource_password" {
  type      = string
  sensitive = true
}
