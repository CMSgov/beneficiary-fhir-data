locals {
  nonsensitive_key_values  = { for key, value in local.yaml : key => value if can(regex("/.*nonsensitive.*/", key)) && value != "UNDEFINED" }
}

resource "aws_ssm_parameter" "nonsensitive_ssm" {
  for_each = local.nonsensitive_key_values

  name      = each.key
  overwrite = true
  type      = "String"
  value     = each.value
}
