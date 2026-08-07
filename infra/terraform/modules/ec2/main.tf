# https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role

# From our deployment sequence:
# 1. Launch an EC2 instance
# 2. The instance needs permission to pull from ECR — so it needs an IAM role
# 3. It needs to know which subnet and security group to live in
# 4. It needs to run a startup script that installs Docker and starts the services

# This means: Generate a policy document that says: EC2 instances are allowed to assume this role.
data "aws_iam_policy_document" "instance_assume_role_policy" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

# fetch amazon linux 2023 latest ami
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# Create a role for the ec2 instance, uses the iam policy document we created earlier
resource "aws_iam_role" "ec2_role" {
  name               = "tickerflow-ec2-${var.environment}"
  assume_role_policy = data.aws_iam_policy_document.instance_assume_role_policy.json
}

# attaches amazon-managed role to iam role
resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# AWS Console hides this, but we can't attach an IAM role to EC2 directly, we need a wrapper (Instance Profile)
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "tickerflow-ec2-profile-${var.environment}"
  role = aws_iam_role.ec2_role.name
}

resource "aws_instance" "ec2_instance" {
  ami                    = data.aws_ssm_parameter.al2023.value
  instance_type          = "t3.small"
  subnet_id              = var.public_subnet_id
  vpc_security_group_ids = [var.instance_sg_id]
  key_name               = var.ec2_key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name
  user_data              = templatefile("user_data.sh", { aws_region = var.aws_region, ecr_registry = var.ec2_registry_url })

  tags = {
    Name = "tickerflow-${var.environment}"
  }
}

