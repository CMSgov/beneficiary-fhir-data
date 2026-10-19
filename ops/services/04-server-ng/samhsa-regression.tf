locals {
  samhsa_regression_repository_default = "bfd-platform-samhsa-regression"
  samhsa_regression_repository_name    = coalesce(var.samhsa_regression_repository_override, local.samhsa_regression_repository_default)
  samhsa_regression_version            = coalesce(var.samhsa_regression_version_override, local.bfd_version)
  samhsa_regression_lambda_name        = "samhsa-regression"
  samhsa_regression_lambda_full_name   = "${local.name_prefix}-${local.samhsa_regression_lambda_name}"
}

data "aws_ecr_image" "samhsa_regression" {
  count = local.samhsa_hook_enabled ? 1 : 0

  repository_name = local.samhsa_regression_repository_name
  image_tag       = local.samhsa_regression_version
}

resource "aws_security_group" "samhsa_regression" {
  count = local.samhsa_hook_enabled ? 1 : 0

  description = "${local.samhsa_regression_lambda_full_name} Lambda security group in ${local.env}"
  name        = "${local.samhsa_regression_lambda_full_name}-sg"
  tags        = { Name = local.samhsa_regression_lambda_full_name }
  vpc_id      = local.vpc.id

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_vpc_security_group_ingress_rule" "samhsa_regression_allow_db_access" {
  for_each = local.samhsa_hook_enabled ? toset(data.aws_security_groups.aurora_cluster.ids) : toset([])

  security_group_id            = each.value
  referenced_security_group_id = one(aws_security_group.samhsa_regression[*].id)
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.samhsa_regression_lambda_full_name} Lambda access to the ${local.env} database"
}

module "log_group_samhsa_regression" {
  source = "../../terraform-modules/general/high-retention-log-group"
  count  = local.samhsa_hook_enabled ? 1 : 0

  name         = "/aws/lambda/${local.samhsa_regression_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "samhsa_regression" {
  depends_on = [aws_iam_role_policy_attachment.samhsa_regression]
  count      = local.samhsa_hook_enabled ? 1 : 0

  function_name = local.samhsa_regression_lambda_full_name
  description   = "Lambda to run the SAMHSA filtering regression suite against the ${local.env} BFD server-ng service"
  tags          = { Name = local.samhsa_regression_lambda_full_name }

  kms_key_arn = local.env_key_arn

  image_uri        = one(data.aws_ecr_image.samhsa_regression[*].image_uri)
  source_code_hash = trimprefix(one(data.aws_ecr_image.samhsa_regression[*].id), "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"

  memory_size = 512
  timeout     = 5 * 60 # 5 minutes

  environment {
    variables = {
      BFD_ENVIRONMENT = local.env
      READER_ENDPOINT = data.aws_rds_cluster.main.reader_endpoint
    }
  }

  vpc_config {
    security_group_ids = [one(aws_security_group.samhsa_regression[*].id)]
    subnet_ids         = local.app_subnets[*].id
  }
  replace_security_groups_on_destroy = true

  role = one(aws_iam_role.samhsa_regression[*].arn)
}
