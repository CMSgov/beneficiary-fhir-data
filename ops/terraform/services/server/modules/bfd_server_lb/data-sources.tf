# accounts
data "aws_caller_identity" "current" {}

data "aws_elb_service_account" "main" {}

# subnets
data "aws_subnet" "app_subnets" {
  count             = length(var.env_config.azs)
  vpc_id            = var.env_config.vpc_id
  availability_zone = var.env_config.azs[count.index]
  filter {
    name   = "tag:Layer"
    values = [var.layer]
  }
}

# S3 bucket for logs
data "aws_s3_bucket" "logs" {
  bucket = var.log_bucket
}
