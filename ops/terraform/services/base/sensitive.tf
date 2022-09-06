locals {
  # NOTE: initial support for ephemeral environments does not include sensitive values; ephemeral.eyaml is a non-existent file
  eyaml_file = contains(local.established_envs, local.env) ? "${local.env}.eyaml" : "ephemeral.eyaml"
  eyaml      = local.is_ephemeral_env ? {} : data.external.eyaml[0].result

  common_sensitive   = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "common") }
  migrator_sensitive = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "migrator") }
  pipeline_sensitive = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "pipeline") }
  server_sensitive   = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "server") }
}

# NOTE: initial support for ephemeral environments does not include sensitive values
data "external" "eyaml" {
  count   = local.is_ephemeral_env ? 0 : 1
  program = ["${path.module}/scripts/read-and-decrypt-eyaml.sh", local.eyaml_file]
}

resource "aws_ssm_parameter" "common_sensitive" {
  for_each = local.common_sensitive

  key_id    = data.aws_kms_key.cmk.arn
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}

resource "aws_ssm_parameter" "migrator_sensitive" {
  for_each = local.migrator_sensitive

  key_id    = data.aws_kms_key.cmk.arn
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}

resource "aws_ssm_parameter" "pipeline_sensitive" {
  for_each = local.pipeline_sensitive

  key_id    = data.aws_kms_key.cmk.arn
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}

resource "aws_ssm_parameter" "server_sensitive" {
  for_each = local.server_sensitive

  key_id    = data.aws_kms_key.cmk.arn
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}
