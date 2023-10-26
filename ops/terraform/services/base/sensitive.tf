locals {
  eyaml = data.external.eyaml.result

  common_sensitive   = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "common") && strcontains(key, "/sensitive/") && value != "UNDEFINED" }
  migrator_sensitive = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "migrator") && strcontains(key, "/sensitive/") && value != "UNDEFINED" }
  pipeline_sensitive = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "pipeline") && strcontains(key, "/sensitive/") && value != "UNDEFINED" }
  server_sensitive   = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "server") && strcontains(key, "/sensitive/") && value != "UNDEFINED" }
  eft_sensitive      = { for key, value in local.eyaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "eft") && strcontains(key, "/sensitive/") && value != "UNDEFINED" }
}

data "external" "eyaml" {
  program = ["${path.module}/scripts/read-and-decrypt-eyaml.sh", local.env, data.aws_kms_key.cmk.arn]
}

resource "aws_ssm_parameter" "common_sensitive" {
  for_each = local.common_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value

  tags = {}
}

resource "aws_ssm_parameter" "migrator_sensitive" {
  for_each = local.migrator_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value

  tags = {}
}

resource "aws_ssm_parameter" "pipeline_sensitive" {
  for_each = local.pipeline_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value

  tags = {}
}

resource "aws_ssm_parameter" "server_sensitive" {
  for_each = local.server_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  tier      = "Intelligent-Tiering"
  value     = each.value

  tags = {}
}

resource "aws_ssm_parameter" "eft_sensitive" {
  for_each = local.eft_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value

  tags = {}
}
