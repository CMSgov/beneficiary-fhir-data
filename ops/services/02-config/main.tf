module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = var.greenfield
  parent_env           = local.parent_env
  environment_name     = terraform.workspace
  service              = local.service
  relative_module_root = "ops/services/02-config"
}

locals {
  service = "config"

  region           = module.terraservice.region
  account_id       = module.terraservice.account_id
  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  is_ephemeral_env = module.terraservice.is_ephemeral_env

  # Terraform v1.5 does not have templatestring, and even if it did templatefile/templatestring
  # require that all templated values be provided. We cannot provide values for ${env} ahead of
  # computing ephemeral config, so this precludes the usage of those functions. However, "templates"
  # in YAML don't need to call any built-in functions or do anything fancy--all "templates" in our
  # YAML should be compatible with envsubst--so we can do our own "templating" with this regex
  # combined with format() and replace()
  template_var_regex = "/\\$\\{{0,1}%s\\}{0,1}/"

  parent_yaml_file     = "${path.module}/values/${local.parent_env}.sops.yaml"
  raw_sops_parent_yaml = file(local.parent_yaml_file)
  # sops cannot decrypt the YAML until the KMS key ARN includes the raw Account ID. We want to
  # protect the ID, so we cannot store it literally in the sops YAML. So, this will take the current
  # account ID and replace the tamplate/placeholder in the YAML to make it valid sops
  valid_sops_parent_yaml  = replace(local.raw_sops_parent_yaml, format(local.template_var_regex, "ACCOUNT_ID"), local.account_id)
  enc_parent_data         = yamldecode(local.valid_sops_parent_yaml)
  sops_key_alias_arn      = local.enc_parent_data.sops.kms[0].arn
  sops_nonsensitive_regex = local.enc_parent_data.sops.unencrypted_regex

  decrypted_parent_data = yamldecode(data.sops_external.this.raw)
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
  env_config = {
    for k, v in local.untemplated_env_config
    # At this point, replace all ${env}/$env with the actual environment name so that the SSM
    # parameter contains the name of its environment as expected
    # TODO: When Greenfield/"next-gen" services are migrated to, remove "/ng/" suffix -- remember to change bfd-terraservice as well!
    : "/ng/${trim(replace(k, format(local.template_var_regex, "env"), local.env), "/")}" => {
      str_val      = replace(v.str_val, format(local.template_var_regex, "env"), local.env)
      is_sensitive = v.is_sensitive
      source       = v.source
    }
  }
}

data "sops_external" "this" {
  source     = local.valid_sops_parent_yaml
  input_type = "yaml"
}

data "aws_kms_key" "sops_key" {
  key_id = local.sops_key_alias_arn
}

resource "aws_ssm_parameter" "this" {
  for_each = local.env_config

  name           = each.key
  tier           = "Intelligent-Tiering"
  value          = each.value.is_sensitive ? each.value.str_val : null
  insecure_value = each.value.is_sensitive ? null : try(nonsensitive(each.value.str_val), each.value.str_val)
  type           = each.value.is_sensitive ? "SecureString" : "String"
  key_id         = each.value.is_sensitive ? data.aws_kms_key.sops_key.id : null

  tags = {
    source_file    = each.value.source
    managed_config = true
  }
}
