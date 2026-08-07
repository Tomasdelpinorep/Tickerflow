variable "aws_region" {
  type    = string
  default = "eu-south-2"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "my_ip" {
  type        = string
}

variable "ec2_key_name" {
  description = "Name of the EC2 key pair for SSH access"
  type        = string
}

variable "db_password" {
  type      = string
  sensitive = true
}