output "ec2_public_ip" {
  description = "SSH: ssh -i <key>.pem ec2-user@<ip>"
  value       = module.ec2.public_ip
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "ecr_registry_url" {
  value = module.ecr.registry_url
}
