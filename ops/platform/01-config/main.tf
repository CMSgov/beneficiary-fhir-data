terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.9"
    }
    sops = {
      source  = "carlpett/sops"
      version = "~> 1.2.0"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/01-config"
}

locals {
  service = "config"

  region       = module.terraservice.region
  account_id   = module.terraservice.account_id
  default_tags = module.terraservice.default_tags
  key_arn      = module.terraservice.key_arn

  platform_yaml_file       = "${path.module}/values/platform.sopsw.yaml"
  raw_sops_platform_yaml   = file(local.platform_yaml_file)
  valid_sops_platform_yaml = data.external.valid_sops_yaml.result.valid_sops
  enc_platform_data        = yamldecode(local.valid_sops_platform_yaml)
  sops_nonsensitive_regex  = local.enc_platform_data.sops.unencrypted_regex

  decrypted_platform_data = yamldecode(data.sops_external.this.raw)
  platform_ssm_config = {
    for key, val in nonsensitive(local.decrypted_platform_data)
    : key => {
      str_val      = tostring(val)
      is_sensitive = length(regexall(local.sops_nonsensitive_regex, key)) == 0
      source       = basename(local.platform_yaml_file)
    } if lower(tostring(val)) != "undefined"
  }
}

data "external" "valid_sops_yaml" {
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
    ${path.module}/scripts/sopsw -c ${local.platform_yaml_file} | yq -o=json '{"valid_sops": (. | tostring)}'
    EOF
  ]
}

data "sops_external" "this" {
  source     = local.valid_sops_platform_yaml
  input_type = "yaml"
}

resource "aws_ssm_parameter" "this" {
  for_each = merge(local.platform_ssm_config, { for k, v in local.platform_ssm_config : "/ng${k}" => v })

  name           = each.key
  tier           = "Intelligent-Tiering"
  value          = each.value.is_sensitive ? each.value.str_val : null
  insecure_value = each.value.is_sensitive ? null : try(nonsensitive(each.value.str_val), each.value.str_val)
  type           = each.value.is_sensitive ? "SecureString" : "String"
  key_id         = each.value.is_sensitive ? local.key_arn : null

  tags = {
    source_file    = each.value.source
    managed_config = true
  }
}
