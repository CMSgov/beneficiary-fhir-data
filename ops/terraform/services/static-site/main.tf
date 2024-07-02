module "terraservice" {
  source = "../_modules/bfd-terraservice"

  environment_name     = terraform.workspace
  relative_module_root = "ops/terraform/services/static-site"
  additional_tags = {
    Layer = local.layer
    Name  = local.full_name
    role  = local.service
  }
}

locals {
  default_tags       = module.terraservice.default_tags
  env                = module.terraservice.env
  seed_env           = module.terraservice.seed_env
  is_ephemeral_env   = module.terraservice.is_ephemeral_env
  latest_bfd_release = module.terraservice.latest_bfd_release
  bfd_version        = var.bfd_version_override == null ? local.latest_bfd_release : var.bfd_version_override

  service   = "static-site"
  legacy_service = local.service
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"

  ssm_hierarchy_roots   = ["bfd"]
  ssm_hierarchies = flatten([
    for root in local.ssm_hierarchy_roots :
    ["/${root}/${local.env}/common", "/${root}/${local.env}/${local.service}"]
  ])
  ssm_flattened_data = {
    names = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : v.names]
    )
    values = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : nonsensitive(v.values)]
    )
  }
  # This returns an object with keys that follow conventional SSM Parameter naming, _excluding_ the
  # nonsensitve/sensitive node. for example, to get a parameter named "vpc_name" in BFD's hierarchy
  # in the "common" service, it would be: local.ssm_config["/bfd/common/vpc_name"]. Or, if the
  # parameter is something more like /dpc/eft/sensitive/inbound/dir, it'd be like:
  # local.ssm_config["/dpc/eft/inbound/dir"]. Essentially, the environment and sensitivity nodes in
  # a given parameter's path are removed to reduce the verbosity of referencing parameters
  # FUTURE: Refactor something like this out into a distinct module much like bfd-terraservice above
  ssm_config = zipmap(
    [
      for name in local.ssm_flattened_data.names :
      replace(name, "/((non)*sensitive|${local.env})//", "")
    ],
    local.ssm_flattened_data.values
  )

  # SSM Lookup
  kms_key_alias        = local.ssm_config["/bfd/common/kms_key_alias"]
  kms_config_key_alias = local.ssm_config["/bfd/common/kms_config_key_alias"]
  vpc_name             = local.ssm_config["/bfd/common/vpc_name"]

  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name
  vpc_id     = data.aws_vpc.this.id
  kms_key_id = data.aws_kms_key.cmk.arn
  kms_config_key_arns = flatten(
    [
      for v in data.aws_kms_key.config_cmk.multi_region_configuration :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
  logging_bucket = "bfd-${local.seed_env}-logs-${local.account_id}"
}
