provider "aws" {
  region = var.aws_region
}

module "vpc" {
  source      = "./modules/vpc"
  environment = var.environment
  my_ip       = var.my_ip
}

module "ecr" {
  source      = "./modules/ecr"
  environment = var.environment
}

module "ec2" {
  source                         = "./modules/ec2"
  ec2_key_name                   = var.ec2_key_name
  ec2_registry_url               = module.ecr.registry_url
  environment                    = var.environment
  instance_sg_id                 = module.vpc.ec2_sg_id
  public_subnet_id               = module.vpc.public_subnet_id
  aws_region                     = var.aws_region
  mailgun_smtp_username          = var.mailgun_smtp_username
  mailgun_smtp_password          = var.mailgun_smtp_password
  personal_email                 = var.personal_email
  spring_kafka_bootstrap_servers = confluent_kafka_cluster.basic.bootstrap_endpoint
  spring_kafka_api_key           = confluent_api_key.cluster_api_key.id
  spring_kafka_api_secret        = confluent_api_key.cluster_api_key.secret
  spring_datasource_url          = "jdbc:postgresql://${module.rds.db_endpoint}/tickerflow"
  spring_datasource_username     = "tickerflow"
  spring_datasource_password     = var.db_password
}

module "rds" {
  source             = "./modules/rds"
  db_password        = var.db_password
  environment        = var.environment
  private_subnet_ids = module.vpc.private_subnet_ids
  rds_sg_id          = module.vpc.rds_sg_id
}
