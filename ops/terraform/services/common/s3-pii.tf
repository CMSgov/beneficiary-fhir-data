## S3 bucket, policy, and KMS key for medicare opt out data

locals {
  name        = "medicare-opt-out"
  log_bucket  = "bfd-${local.env}-${local.name}-${local.account_id}"
  read_arns   = split(" ", data.aws_ssm_parameter.medicare_opt_out_config_read_roles.value)
  write_accts = split(" ", data.aws_ssm_parameter.medicare_opt_out_config_write_accts.value)
  admin_users = split(" ", data.aws_ssm_parameter.medicare_opt_out_config_admin_users.value)
}

module "medicare_opt_out" {
  count  = local.is_ephemeral_env ? 0 : 1
  source = "../../modules/resources/s3_pii"
  env    = local.env

  pii_bucket_config = {
    name        = local.name
    log_bucket  = local.logging_bucket
    read_arns   = local.read_arns
    write_accts = local.write_accts
    admin_arns  = local.admin_users
    account_id  = local.account_id
  }
}
