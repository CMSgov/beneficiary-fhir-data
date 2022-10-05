locals {
  eyaml_file = local.is_ephemeral_env ? "ephemeral.eyaml" : "${local.env}.eyaml"
  eyaml      = data.external.eyaml.result

  common_sensitive   = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "common") && value != "UNDEFINED" }
  migrator_sensitive = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "migrator") && value != "UNDEFINED" }
  pipeline_sensitive = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "pipeline") && value != "UNDEFINED" }
  server_sensitive   = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "server") && value != "UNDEFINED" }
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
