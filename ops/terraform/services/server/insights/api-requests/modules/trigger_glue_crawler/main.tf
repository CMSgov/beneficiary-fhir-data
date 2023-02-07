locals {
  env = terraform.workspace

  lambda_full_name = "${var.name_prefix}-trigger-glue-crawler"
}
