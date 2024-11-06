locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name
  alt_region = data.aws_region.alt.name

  env = "mgmt"
  established_envs = [
    "test",
    "prod-sbx",
    "prod"
  ]

  bfd_insights_kms_key_id = data.aws_kms_key.insights.arn
  kms_key_id              = data.aws_kms_key.cmk.arn
  tf_state_kms_key_id     = data.aws_kms_key.tf_state.arn
  test_kms_key_id         = aws_kms_key.data_keys["test"].arn
  prod_sbx_kms_key_id     = aws_kms_key.data_keys["prod-sbx"].arn
  prod_kms_key_id         = aws_kms_key.data_keys["prod"].arn

  # BFD-3520
  log_groups = {
    cloudinit_out = "/aws/ec2/var/log/cloud-init-output.log"
  }
  init_fail_pattern     = "%failed=[1-9]%"
  this_metric_namespace = "bfd-${local.env}/ec2"
  init_fail_filter_name = "bfd-${local.env}/ec2/init-count/fail"
  init_fail_metric_name = "cloudinit/count/fail"
  init_fail_alarm_name  = "bfd-${local.env}-cloud-init-failure"
  #

  all_kms_data_key_arns = concat(values(aws_kms_key.data_keys)[*].arn, values(aws_kms_key.data_keys_alt)[*].arn)
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

  ssm_hierarchies = ["/bfd/${local.env}/common"]
  ssm_flattened_data = {
    names = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : v.names]
    )
    values = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : nonsensitive(v.values)]
    )
  }
  ssm_config = zipmap(
    [
      for name in local.ssm_flattened_data.names :
      replace(name, "/((non)*sensitive|${local.env})//", "")
    ],
    local.ssm_flattened_data.values
  )
}
