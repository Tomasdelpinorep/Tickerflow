terraform {
  required_version = ">= 1.7"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

module "vpc" {
  source      = "modules/vpc"
  environment = var.environment
  my_ip       = var.my_ip
}

module "ecr" {
  source       = "modules/ecr"
  environment  = var.environment
}

module "ec2" {
  source           = "modules/ec2"
  ec2_key_name     = var.ec2_key_name
  ec2_registry_url = module.ecr.registry_url
  environment      = var.environment
  instance_sg_id   = module.vpc.ec2_sg_id
  public_subnet_id = module.vpc.public_subnet_id
  aws_region       = var.aws_region
}

module "rds" {
  source      = "modules/rds"
  db_password = var.db_password
  environment = var.environment
  private_subnet_ids = module.vpc.private_subnet_ids
  rds_sg_id   = module.vpc.rds_sg_id
}