variable "environment"{
  type = string
}

variable "private_subnet_ids"{
  type = list(string)
}

variable "db_password" {
  type = string
}

variable "rds_sg_id" {
  type = string
}