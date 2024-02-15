locals {
  sensitive_key_values   = { for key, value in local.yaml : key => value if can(regex("/.*sensitive.*/", key)) && value != "UNDEFINED" }
}

resource "aws_ssm_parameter" "sensitive_ssm" {
  for_each = local.sensitive_key_values

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}
