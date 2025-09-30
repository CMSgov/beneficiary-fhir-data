terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
    sops = {
      source  = "carlpett/sops"
      version = "~> 1.2.0"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  service              = local.service
  relative_module_root = "ops/services/01-config"
}

locals {
  service = "config"

  region       = module.terraservice.region
  account_id   = module.terraservice.account_id
  default_tags = module.terraservice.default_tags
  key_arn      = module.terraservice.key_arn

  sops_config_yaml        = yamldecode(file("${path.module}/values/.sops.yaml"))
  sops_nonsensitive_regex = local.sops_config_yaml.creation_rules[0].unencrypted_regex

  root_yaml_file       = "${path.module}/values/platform.root.sopsw.yaml"
  valid_root_sops_yaml = data.external.root_sops_yaml.result.valid_sops

  account_yaml_file       = "${path.module}/values/platform.${local.account_type}.sopsw.yaml"
  valid_account_sops_yaml = data.external.account_sops_yaml.result.valid_sops

  decrypted_root_data    = yamldecode(data.sops_external.root.raw)
  decrypted_account_data = yamldecode(data.sops_external.account.raw)

  root_ssm_config = {
    for key, val in nonsensitive(local.decrypted_root_data)
    : key => {
      str_val      = tostring(val)
      is_sensitive = length(regexall(local.sops_nonsensitive_regex, key)) == 0
      source       = basename(local.root_yaml_file)
    } if lower(tostring(val)) != "undefined"
  }
  account_ssm_config = {
    for key, val in nonsensitive(local.decrypted_account_data)
    : key => {
      str_val      = tostring(val)
      is_sensitive = length(regexall(local.sops_nonsensitive_regex, key)) == 0
      source       = basename(local.account_yaml_file)
    } if lower(tostring(val)) != "undefined"
  }
  # Account-specific keys take precedence over root ("common") YAML
  platform_ssm_config = merge(local.root_ssm_config, local.account_ssm_config)
}

data "external" "root_sops_yaml" {
  # sops (not sopsw, our custom wrapper) cannot decrypt the YAML until the KMS key ARNs include the
  # Account ID and the sops metadata block includes valid "lastmodified" and "mac" properties. We
  # need to use sopsw's "-c/--cat" function to construct a valid sops file so that it can be
  # consumed by the sops provider
  program = [
    "bash",
    "-c",
    # Allows us to pipe to yq so that sopsw does not need to emit JSON to work with this external
    # data source
    <<-EOF
    ${path.module}/scripts/sopsw -c ${local.root_yaml_file} | yq -o=json '{"valid_sops": (. | tostring)}'
    EOF
  ]
}

data "external" "account_sops_yaml" {
  program = [
    "bash",
    "-c",
    <<-EOF
    ${path.module}/scripts/sopsw -c ${local.account_yaml_file} | yq -o=json '{"valid_sops": (. | tostring)}'
    EOF
  ]
}

data "sops_external" "root" {
  source     = local.valid_root_sops_yaml
  input_type = "yaml"
}

data "sops_external" "account" {
  source     = local.valid_account_sops_yaml
  input_type = "yaml"
}

resource "aws_ssm_parameter" "this" {
  for_each = local.platform_ssm_config

  name   = each.key
  tier   = "Intelligent-Tiering"
  value  = each.value.str_val
  type   = each.value.is_sensitive ? "SecureString" : "String"
  key_id = each.value.is_sensitive ? local.key_arn : null

  tags = {
    source_file    = each.value.source
    managed_config = true
  }
}
