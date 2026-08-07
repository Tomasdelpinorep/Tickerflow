data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

output "registry_url" {
  value = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.amazonaws.com"
}