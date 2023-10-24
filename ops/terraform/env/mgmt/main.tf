locals {
  account_id = data.aws_caller_identity.current.account_id
  env        = "mgmt"
  established_envs = [
    "test",
    "prod-sbx",
    "prod"
  ]

  bfd_insights_kms_key_id = data.aws_kms_key.insights.arn
  kms_key_id              = data.aws_kms_key.cmk.arn
  tf_state_kms_key_id     = data.aws_kms_key.tf_state.arn
  test_kms_key_id         = data.aws_kms_key.test_cmk.arn
  prod_sbx_kms_key_id     = data.aws_kms_key.prod_sbx_cmk.arn
  prod_kms_key_id         = data.aws_kms_key.prod_cmk.arn

  all_kms_config_key_arns = flatten(
    [
      for v in concat(
        data.aws_kms_key.config_cmk.multi_region_configuration,
        data.aws_kms_key.test_config_cmk.multi_region_configuration,
        data.aws_kms_key.prod_sbx_config_cmk.multi_region_configuration,
        data.aws_kms_key.prod_config_cmk.multi_region_configuration
      ) :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )

  sensitive_common_config = zipmap(
    [
      for name in data.aws_ssm_parameters_by_path.common_sensitive.names :
      element(split("/", name), length(split("/", name)) - 1)
    ],
    nonsensitive(data.aws_ssm_parameters_by_path.common_sensitive.values)
  )

  retention_policies = {
    for param in data.aws_ssm_parameters_by_path.retention_policies :
    element(split("/", param.name), length(split("/", param.name)) - 1) => param.value
  }
}

data "aws_caller_identity" "current" {}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-mgmt-cmk"
}

data "aws_kms_key" "test_cmk" {
  key_id = "alias/bfd-test-cmk"
}

data "aws_kms_key" "prod_sbx_cmk" {
  key_id = "alias/bfd-prod-sbx-cmk"
}

data "aws_kms_key" "prod_cmk" {
  key_id = "alias/bfd-prod-cmk"
}

data "aws_kms_key" "config_cmk" {
  key_id = "alias/bfd-mgmt-config-cmk"
}

data "aws_kms_key" "test_config_cmk" {
  key_id = "alias/bfd-test-config-cmk"
}

data "aws_kms_key" "prod_sbx_config_cmk" {
  key_id = "alias/bfd-prod-sbx-config-cmk"
}

data "aws_kms_key" "prod_config_cmk" {
  key_id = "alias/bfd-prod-config-cmk"
}

data "aws_kms_key" "tf_state" {
  key_id = "alias/bfd-tf-state"
}

data "aws_kms_key" "insights" {
  key_id = "alias/bfd-insights-bfd-cmk"
}

# TODO: As of late July 2022, this is parameter is manually managed.
data "aws_ssm_parameter" "cbc_aws_account_arn" {
  name            = "/bfd/mgmt/jenkins/sensitive/cbc_aws_account_arn"
  with_decryption = true
}

# The root ARN of the AWS account that manages the CPM service
# TODO: As of 02/2023 this parameter is manually managed
data "aws_ssm_parameter" "cpm_aws_account_arn" {
  name            = "/bfd/mgmt/jenkins/sensitive/cpm_aws_account_arn"
  with_decryption = true
}

data "aws_ssm_parameters_by_path" "common_sensitive" {
  path = "/bfd/${local.env}/common/sensitive"
}
