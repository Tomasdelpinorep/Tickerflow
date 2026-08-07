# Tell AWS in which subnets it can put the database in
resource "aws_db_subnet_group" "main" {
  name = "tickerflow-${var.environment}"

  # remember: this property is written in plural because rds needs a minimum of two subnets in different az's to run
  subnet_ids = var.private_subnet_ids

  tags = { Name = "tickerflow-rds-subnet-group-${var.environment}" }
}

# Indicate that we want postgres, instance type for the db, credentials, subnet group, apply security group and config
resource aws_db_instance "postgres" {
  identifier = "tickerflow-${var.environment}"
  engine = "postgres"
  engine_version = "16"
  instance_class = "db.t3.micro"
  allocated_storage = 20
  storage_type = "gp2"

  db_name = "tickerflow"
  username = "tickerflow"
  password = var.db_password

  db_subnet_group_name = aws_db_subnet_group.main.name

  # apply security group
  vpc_security_group_ids = [var.rds_sg_id]

  publicly_accessible = false
  multi_az            = false
  skip_final_snapshot = true

  tags = { Environment = var.environment }
}