variable "environment" {
  type = string
}

variable "untagged_image_expiry_days" {
  type        = number
  description = "Expire untagged images older than this many days"
  default     = 7
}