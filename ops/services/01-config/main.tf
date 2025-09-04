terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.9"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/01-config"
}

locals {
  service = "config"

  region           = module.terraservice.region
  account_id       = module.terraservice.account_id
  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  is_ephemeral_env = module.terraservice.is_ephemeral_env
  env_key_arn      = module.terraservice.env_key_arn

  # templatefile/templatestring require that all templated values be provided. We cannot provide
  # values for ${env} ahead of computing ephemeral config, so this alone makes it impossible to use
  # those functions. However, "templates" in YAML don't need to call any built-in functions or do
  # anything fancy--all "templates" in our YAML should be compatible with envsubst--so we can do our
  # own "templating" with this regex combined with format() and replace()
  template_var_regex = "/\\$\\{{0,1}%s\\}{0,1}/"

  parent_yaml_file        = "${path.module}/values/${local.parent_env}.sopsw.yaml"
  raw_sops_parent_yaml    = file(local.parent_yaml_file)
  enc_parent_data         = yamldecode(local.raw_sops_parent_yaml)
  sops_nonsensitive_regex = local.enc_parent_data.sops.unencrypted_regex

  decrypted_parent_data = yamldecode(data.external.decrypted_sops.result.decrypted_sops)
  parent_ssm_config = {
    for key, val in nonsensitive(local.decrypted_parent_data)
    : key => {
      str_val      = tostring(val)
      is_sensitive = length(regexall(local.sops_nonsensitive_regex, key)) == 0
      source       = basename(local.parent_yaml_file)
    } if lower(tostring(val)) != "undefined"
  }

  ephemeral_yaml_file = "${path.module}/values/ephemeral.yaml"
  ephemeral_data      = yamldecode(file(local.ephemeral_yaml_file))
  ephemeral_to_copy = [
    for key in keys(local.parent_ssm_config)
    # Using anytrue+strcontains to enable recursive copying from the parent environment, e.g.
    # client_certificates hierarchy
    : key if anytrue([for copy_key in local.ephemeral_data.copy : strcontains(key, copy_key)])
  ]
  ephemeral_vals = {
    for key, val in local.ephemeral_data.values
    : key => {
      str_val      = tostring(val)
      is_sensitive = false
      source       = basename(local.ephemeral_yaml_file)
    } if lower(tostring(val)) != "undefined"
  }

  untemplated_env_config = local.is_ephemeral_env ? merge(
    # First, copy the values specified in ephemeral "copy" from the parent env's configuration
    {
      for k, v in local.parent_ssm_config
      : k => v if contains(local.ephemeral_to_copy, k)
    },
    # Then, take any ephemeral default values. These take precedence in case a parameter was
    # erroneously specified for copying
    local.ephemeral_vals
  ) : local.parent_ssm_config
  # TODO: Remove
  ng_env_config = !var.greenfield ? {
    for k, v in local.untemplated_env_config
    # At this point, replace all ${env}/$env with the actual environment name so that the SSM
    # parameter contains the name of its environment as expected
    : "/ng/${trim(replace(k, format(local.template_var_regex, "env"), local.env), "/")}" => {
      str_val      = replace(v.str_val, format(local.template_var_regex, "env"), local.env)
      is_sensitive = v.is_sensitive
      source       = v.source
    }
  } : {}
  # TODO: Remove merge() when ng_env_config is removed
  env_config = merge({
    for k, v in local.untemplated_env_config
    # At this point, replace all ${env}/$env with the actual environment name so that the SSM
    # parameter contains the name of its environment as expected
    : replace(k, format(local.template_var_regex, "env"), local.env) => {
      str_val      = replace(v.str_val, format(local.template_var_regex, "env"), local.env)
      is_sensitive = v.is_sensitive
      source       = v.source
    }
  }, local.ng_env_config)
}

data "external" "decrypted_sops" {
  program = [
    "bash",
    "-c",
    # Allows us to pipe to yq so that sopsw does not need to emit JSON to work with this external
    # data source
    <<-EOF
    ${path.module}/scripts/sopsw -d ${local.parent_yaml_file} | yq -o=json '{"decrypted_sops": (. | tostring)}'
    EOF
  ]
}

resource "aws_ssm_parameter" "this" {
  for_each = local.env_config

  name           = each.key
  tier           = "Intelligent-Tiering"
  value          = each.value.str_val
  type           = each.value.is_sensitive ? "SecureString" : "String"
  key_id         = each.value.is_sensitive ? local.env_key_arn : null

  tags = {
    source_file    = each.value.source
    managed_config = true
  }
}
