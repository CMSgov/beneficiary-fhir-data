#
# Build the stateful resources for an environment.
#
# This script also builds the associated KMS needed by the stateful and stateless resources
#

locals {
  azs               = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config        = { env = var.env_config.env, tags = var.env_config.tags, vpc_id = data.aws_vpc.main.id, zone_id = module.local_zone.zone_id }
  is_prod           = substr(var.env_config.env, 0, 4) == "prod"
  victor_ops_url    = var.victor_ops_url
  enable_victor_ops = local.is_prod # only wake people up for prod alarms
}

# VPC
#
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-vpc"]
  }
}

# KMS 
#
# The customer master key is created outside of this script
#
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${var.env_config.env}-cmk"
}

# Subnets
# 
# Subnets are created by CCS VPC setup
#
data "aws_subnet" "data_subnets" {
  count             = length(local.azs)
  vpc_id            = data.aws_vpc.main.id
  availability_zone = local.azs[count.index]
  filter {
    name   = "tag:Layer"
    values = ["data"]
  }
}

# Other Security Groups
#
# Find the security group for the Cisco VPN
#
data "aws_security_group" "vpn" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-vpn-private"]
  }
}

# Find the management group
#
data "aws_security_group" "tools" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-enterprise-tools"]
  }
}

# Find the tools group 
#
data "aws_security_group" "management" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-remote-management"]
  }
}

#
# Start to build things
#

# DNS
#
# Build a VPC private local zone for CNAME records
#
module "local_zone" {
  source     = "../resources/dns"
  env_config = { env = var.env_config.env, tags = var.env_config.tags, vpc_id = data.aws_vpc.main.id }
  public     = false
}

# CloudWatch SNS Topic
#
resource "aws_sns_topic" "cloudwatch_alarms" {
  name         = "bfd-${var.env_config.env}-cloudwatch-alarms"
  display_name = "BFD Cloudwatch Alarm. Created by Terraform."
  tags         = var.env_config.tags
}

resource "aws_sns_topic_subscription" "alarm" {
  count                  = local.enable_victor_ops ? 1 : 0
  protocol               = "https"
  topic_arn              = aws_sns_topic.cloudwatch_alarms.arn
  endpoint               = local.victor_ops_url
  endpoint_auto_confirms = true
}

resource "aws_sns_topic" "cloudwatch_ok" {
  name         = "bfd-${var.env_config.env}-cloudwatch-ok"
  display_name = "BFD Cloudwatch OK notifications. Created by Terraform."
  tags         = var.env_config.tags
}

resource "aws_sns_topic_subscription" "ok" {
  count                  = local.enable_victor_ops ? 1 : 0
  topic_arn              = aws_sns_topic.cloudwatch_ok.arn
  protocol               = "https"
  endpoint               = local.victor_ops_url
  endpoint_auto_confirms = true
}

# Aurora module: supplants separate param groups, rds modules, and rds
# alarms modules
#
module "aurora" {
  source             = "../resources/aurora"
  module_features    = var.module_features
  env_config         = local.env_config
  aurora_config      = var.aurora_config
  aurora_node_params = var.aurora_node_params
  stateful_config = {
    azs        = local.azs
    subnet_ids = [for s in data.aws_subnet.data_subnets : s.id]
    kms_key_id = data.aws_kms_key.master_key.arn
    vpc_sg_ids = [
      data.aws_security_group.vpn.id,
      data.aws_security_group.tools.id,
      data.aws_security_group.management.id
    ]
  }
}

# S3 Admin bucket for adminstrative stuff
#
module "admin" {
  source     = "../resources/s3"
  role       = "admin"
  env_config = local.env_config
  kms_key_id = data.aws_kms_key.master_key.arn
  log_bucket = module.logs.id
}

# S3 bucket for logs 
#
module "logs" {
  source     = "../resources/s3"
  role       = "logs"
  env_config = local.env_config
  acl        = "log-delivery-write" # For AWS bucket logs
  kms_key_id = null                 # Use AWS encryption to support AWS Agents writing to this bucket
}

# S3 bucket for ETL files
#
module "etl" {
  source     = "../resources/s3"
  role       = "etl"
  env_config = local.env_config
  kms_key_id = data.aws_kms_key.master_key.arn
  log_bucket = module.logs.id
}

# IAM policy, user, and attachment to allow external read-write
# access to ETL bucket
#
# NOTE: We only need this for production, however it is ok to
# provision these resources for all environments since the mechanism
# by which we control access is through a manually provisioned
# access key
#
resource "aws_iam_policy" "etl_rw_s3" {
  name        = "bfd-${local.env_config.env}-etl-rw-s3"
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
      "Resource": ["${module.etl.arn}"]
    },
    {
      "Sid": "ETLRWBucketActions",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": ["${module.etl.arn}/*"]
    }
  ]
}
EOF
}

resource "aws_iam_user" "etl" {
  name = "bfd-${local.env_config.env}-etl"
}

resource "aws_iam_user_policy_attachment" "etl_rw_s3" {
  user       = aws_iam_user.etl.name
  policy_arn = aws_iam_policy.etl_rw_s3.arn
}

# S3 bucket, policy, and KMS key for medicare opt out data
#
module "medicare_opt_out" {
  source     = "../resources/s3_pii"
  env_config = local.env_config

  pii_bucket_config = {
    name        = "medicare-opt-out"
    log_bucket  = module.logs.id
    read_arns   = var.medicare_opt_out_config.read_roles
    write_accts = var.medicare_opt_out_config.write_accts
    admin_arns  = var.medicare_opt_out_config.admin_users
  }
}

# CloudWatch Log Groups
#
resource "aws_cloudwatch_log_group" "var_log_messages" {
  name       = "/bfd/${var.env_config.env}/var/log/messages"
  kms_key_id = data.aws_kms_key.master_key.arn
  tags       = local.env_config.tags
}

resource "aws_cloudwatch_log_group" "var_log_secure" {
  name       = "/bfd/${var.env_config.env}/var/log/secure"
  kms_key_id = data.aws_kms_key.master_key.arn
  tags       = local.env_config.tags
}

resource "aws_cloudwatch_log_group" "bfd_pipeline_messages_txt" {
  name       = "/bfd/${var.env_config.env}/bfd-pipeline/messages.txt"
  kms_key_id = data.aws_kms_key.master_key.arn
  tags       = var.env_config.tags
}

resource "aws_cloudwatch_log_group" "bfd_server_access_txt" {
  name       = "/bfd/${var.env_config.env}/bfd-server/access.txt"
  kms_key_id = data.aws_kms_key.master_key.arn
  tags       = var.env_config.tags
}

resource "aws_cloudwatch_log_group" "bfd_server_access_json" {
  name       = "/bfd/${var.env_config.env}/bfd-server/access.json"
  kms_key_id = data.aws_kms_key.master_key.arn
  tags       = var.env_config.tags
}

resource "aws_cloudwatch_log_group" "bfd_server_messages_json" {
  name       = "/bfd/${var.env_config.env}/bfd-server/messages.json"
  kms_key_id = data.aws_kms_key.master_key.arn
  tags       = var.env_config.tags
}

resource "aws_cloudwatch_log_group" "bfd_server_newrelic_agent" {
  name       = "/bfd/${var.env_config.env}/bfd-server/newrelic_agent.log"
  kms_key_id = data.aws_kms_key.master_key.arn
  tags       = var.env_config.tags
}

resource "aws_cloudwatch_log_group" "bfd_server_gc" {
  name       = "/bfd/${var.env_config.env}/bfd-server/gc.log"
  kms_key_id = data.aws_kms_key.master_key.arn
  tags       = var.env_config.tags
}
