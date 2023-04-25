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

# kms master key
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${local.env}-cmk"
}
