locals {
  samhsa_hook_enabled = !local.is_ephemeral_env || var.ephemeral_samhsa_hook_override

  sh_repository_default = "bfd-platform-samhsa-regression"
  sh_repository_name    = coalesce(var.samhsa_hook_repository_override, local.sh_repository_default)
  sh_version            = coalesce(var.samhsa_hook_version_override, local.bfd_version)
  sh_lambda_name        = "samhsa-hook"
  sh_lambda_full_name   = "${local.name_prefix}-${local.sh_lambda_name}"
  sh_lambda_src         = replace(local.sh_lambda_name, "-", "_")
}

data "aws_ecr_image" "samhsa_hook" {
  repository_name = local.sh_repository_name
  image_tag       = local.sh_version
}

module "log_group_samhsa_hook" {
  source       = "../../terraform-modules/general/high-retention-log-group"
  name         = "/aws/lambda/${local.sh_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "samhsa_hook" {
  depends_on = [aws_iam_role_policy_attachment.samhsa_hook]
  count      = local.samhsa_hook_enabled ? 1 : 0

  function_name = local.sh_lambda_full_name
  description = join("", [
    "Invoked on the POST_TEST_TRAFFIC_SHIFT ECS Lifecycle Event. This Lambda runs the SAMHSA ",
    "regression test suite against the replacement tasks during deployment"
  ])
  tags = {
    Name    = local.sh_lambda_full_name,
    version = local.sh_version
  }

  kms_key_arn = local.env_key_arn

  image_uri        = data.aws_ecr_image.samhsa_hook.image_uri
  source_code_hash = trimprefix(data.aws_ecr_image.samhsa_hook.id, "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"

  memory_size = 128
  timeout     = 90

  logging_config {
    log_format = "Text"
    log_group  = module.log_group_samhsa_hook.name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT = local.env
      HOSTNAME        = "https://${data.aws_network_interface.load_balancer.private_ip}:${aws_lb_listener.this[local.green_state].port}"
      # TODO: Add DB_CONN_STR
    }
  }

  role = aws_iam_role.samhsa_hook.arn
}
