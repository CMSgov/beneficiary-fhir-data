data "aws_caller_identity" "current" {}

data "aws_elb_service_account" "this" {}

data "aws_vpc" "this" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpc"]
  }
}

data "aws_subnet" "this" {
  for_each          = toset(local.azs)
  vpc_id            = local.vpc_id
  availability_zone = each.key
  filter {
    name   = "tag:Layer"
    values = [local.layer]
  }
}

data "aws_s3_bucket" "logs" {
  bucket = "bfd-${local.env}-logs-${local.account_id}"
}
