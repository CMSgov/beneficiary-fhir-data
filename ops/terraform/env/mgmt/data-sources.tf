
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

data "aws_region" "alt" {
  provider = aws.alt
}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-mgmt-cmk"
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

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}
