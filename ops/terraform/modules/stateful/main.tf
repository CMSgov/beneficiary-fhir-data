## 
# Stateful resources for an environment and associated KMS needed by both stateful and stateless resources

locals {
  account_id = data.aws_caller_identity.current.account_id
  azs        = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env        = var.env
  env_config = { env = local.env, vpc_id = data.aws_vpc.main.id, zone_id = module.local_zone.zone_id }
}

data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "logs" {
  bucket = "bfd-${local.env}-logs-${local.account_id}"
}

data "aws_s3_bucket" "etl" {
  bucket = "bfd-${local.env}-etl-${local.account_id}"
}

# vpc
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpc"]
  }
}

# kms 
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${local.env}-cmk"
}

## VPC Private Local Zone for CNAME Records
#
module "local_zone" {
  source     = "../resources/dns"
  env_config = { env = var.env, vpc_id = data.aws_vpc.main.id }
  public     = false
}



## IAM policy, user, and attachment to allow external read-write access to ETL bucket
# NOTE: We only need this for production, however it is ok to
# provision these resources for all environments since the mechanism
# by which we control access is through a manually provisioned
# access key
resource "aws_iam_policy" "etl_rw_s3" {
  name        = "bfd-${local.env}-etl-rw-s3"
  description = "ETL read-write S3 policy"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ETLRWKMS",
      "Action": ["kms:Decrypt"],
      "Effect": "Allow",
      "Resource": ["${data.aws_kms_key.master_key.arn}"]
    },
    {
      "Sid": "ETLRWBucketList",
      "Action": ["s3:ListBucket"],
      "Effect": "Allow",
      "Resource": ["${data.aws_s3_bucket.etl.arn}"]
    },
    {
      "Sid": "ETLRWBucketActions",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": ["${data.aws_s3_bucket.etl.arn}/*"]
    }
  ]
}
EOF
}

## S3 bucket, policy, and KMS key for medicare opt out data
#
module "medicare_opt_out" {
  source     = "../resources/s3_pii"
  env_config = local.env_config

  pii_bucket_config = {
    name        = "medicare-opt-out"
    log_bucket  = data.aws_s3_bucket.logs.id
    read_arns   = var.medicare_opt_out_config.read_roles
    write_accts = var.medicare_opt_out_config.write_accts
    admin_arns  = var.medicare_opt_out_config.admin_users
  }
}

## CloudWatch Log Groups
#
resource "aws_cloudwatch_log_group" "var_log_messages" {
  name       = "/bfd/${local.env}/var/log/messages"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "var_log_secure" {
  name       = "/bfd/${local.env}/var/log/secure"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_access_json" {
  name       = "/bfd/${local.env}/bfd-server/access.json"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_messages_json" {
  name       = "/bfd/${local.env}/bfd-server/messages.json"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_newrelic_agent" {
  name       = "/bfd/${local.env}/bfd-server/newrelic_agent.log"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_gc" {
  name       = "/bfd/${local.env}/bfd-server/gc.log"
  kms_key_id = data.aws_kms_key.master_key.arn
}
