module "terraservice" {
  source = "../_modules/bfd-terraservice"

  environment_name     = terraform.workspace
  relative_module_root = "ops/terraform/services/static-site"
  additional_tags      = local.static_tags
}

locals {
  default_tags = merge(module.terraservice.default_tags, local.static_tags)
  static_tags = {
    Layer = local.layer
    Name  = local.full_name
    role  = local.service
    SaaS  = "Cloudfront"
  }
  env                = module.terraservice.env
  seed_env           = module.terraservice.seed_env
  is_ephemeral_env   = module.terraservice.is_ephemeral_env
  latest_bfd_release = module.terraservice.latest_bfd_release
  bfd_version        = var.bfd_version_override == null ? local.latest_bfd_release : var.bfd_version_override

  service   = "static-site"
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"

  ssm_hierarchy_roots = ["bfd"]
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

  kms_config_key_arns = flatten(
    [
      for v in data.aws_kms_key.config_cmk.multi_region_configuration :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
  logging_bucket = "bfd-${local.seed_env}-logs"

  root_domain_name       = data.aws_ssm_parameter.zone_name.value
  static_cloudfront_name = "bfd-${local.env}-static"
  static_logging_name    = "bfd-${local.env}-staticlogging"
  static_site_fqdn_by_env = {
    prod = local.root_domain_name
    test = "${local.env}.${local.root_domain_name}"
  }
  # If the environment is not prod or test, then we must default to the CloudFront distribution URL.
  # This is specified by null, and the resulting CNAME will be conditionally created depending on
  # whether this value exists
  static_site_fqdn = lookup(local.static_site_fqdn_by_env, local.env, null)

  kms_key_id = data.aws_kms_key.data_cmk.arn

  # BFD-3588
  acm_cert_body        = local.ssm_config["/bfd/${local.service}/tls_certificate_body"]
  acm_private_key_body = local.ssm_config["/bfd/${local.service}/tls_private_key_body"]
}

# BFD-3588
resource "aws_acm_certificate" "env_issued" {
  private_key      = local.acm_private_key_body
  certificate_body = local.acm_cert_body
}
