locals {
  is_prod = var.env_config.env == "prod"

  log_groups = {
    messages = "/bfd/${var.env_config.env}/bfd-pipeline/messages.txt"
  }

  pipeline_messages_error = {
    period       = "300"
    eval_periods = "10"
    threshold    = "0"
    datapoints   = "10"
  }

  pipeline_messages_datasetfailed = {
    period       = "300"
    eval_periods = "1"
    threshold    = "0"
    datapoints   = "1"
  }

  alarm_actions = var.alarm_notification_arn == null ? [] : [var.alarm_notification_arn]
  ok_actions    = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]
}

# Locate the S3 bucket that stores the RIF data to be processed by the BFD Pipeline application.
#
data "aws_s3_bucket" "rif" {
  bucket = "bfd-${var.env_config.env}-etl-${var.launch_config.account_id}"
}

# Locate the customer master key for this environment.
#
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${var.env_config.env}-cmk"
}

# CloudWatch metric filters
#
resource "aws_cloudwatch_log_metric_filter" "pipeline-messages-error-count" {
  name           = "bfd-${var.env_config.env}/bfd-pipeline/messages/count/error"
  pattern        = "[date, time, java_thread, level = \"ERROR\", java_class, message]"
  log_group_name = local.log_groups.messages

  metric_transformation {
    name          = "messages/count/error"
    namespace     = "bfd-${var.env_config.env}/bfd-pipeline"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "pipeline-messages-datasetfailed-count" {
  name           = "bfd-${var.env_config.env}/bfd-pipeline/messages/count/datasetfailed"
  pattern        = "[date, time, java_thread, level = \"ERROR\", java_class, message = \"*Data set failed with an unhandled error*\"]"
  log_group_name = local.log_groups.messages

  metric_transformation {
    name          = "messages/count/datasetfailed"
    namespace     = "bfd-${var.env_config.env}/bfd-pipeline"
    value         = "1"
    default_value = "0"
  }
}

# CloudWatch metric alarms
#
resource "aws_cloudwatch_metric_alarm" "pipeline-messages-error" {
  alarm_name          = "bfd-${var.env_config.env}-pipeline-messages-error"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.pipeline_messages_error.eval_periods
  period              = local.pipeline_messages_error.period
  statistic           = "Sum"
  threshold           = local.pipeline_messages_error.threshold
  alarm_description   = "Pipeline errors detected over ${local.pipeline_messages_error.eval_periods} evaluation periods of ${local.pipeline_messages_error.period} seconds in APP-ENV: bfd-${var.env_config.env}"

  metric_name = "messages/count/error"
  namespace   = "bfd-${var.env_config.env}/bfd-pipeline"

  alarm_actions = local.is_prod ? local.alarm_actions : []
  ok_actions    = local.is_prod ? local.ok_actions : []

  datapoints_to_alarm = local.pipeline_messages_error.datapoints
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "pipeline-messages-datasetfailed" {
  alarm_name          = "bfd-${var.env_config.env}-pipeline-messages-datasetfailed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.pipeline_messages_datasetfailed.eval_periods
  period              = local.pipeline_messages_datasetfailed.period
  statistic           = "Sum"
  threshold           = local.pipeline_messages_datasetfailed.threshold
  alarm_description   = "Data set processing failed, pipeline has shut down in APP-ENV: bfd-${var.env_config.env}"

  metric_name = "messages/count/datasetfailed"
  namespace   = "bfd-${var.env_config.env}/bfd-pipeline"

  alarm_actions = local.is_prod ? local.alarm_actions : []
  ok_actions    = local.is_prod ? local.ok_actions : []

  datapoints_to_alarm = local.pipeline_messages_datasetfailed.datapoints
  treat_missing_data  = "notBreaching"
}

# Security group for application-specific (i.e. non-management) traffic.
#
resource "aws_security_group" "app" {
  name        = "bfd-${var.env_config.env}-etl-app"
  description = "Access specific to the BFD Pipeline application."
  vpc_id      = var.env_config.vpc_id
  tags        = merge({ Name = "bfd-${var.env_config.env}-etl-app" }, var.env_config.tags)

  # Note: The application does not currently listen on any ports, so no ingress rules are needed.

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# App access to the database
#
resource "aws_security_group_rule" "allow_db_primary_access" {
  type        = "ingress"
  from_port   = 5432
  to_port     = 5432
  protocol    = "tcp"
  description = "Allows BFD Pipeline access to the primary DB."

  security_group_id        = var.db_config.db_sg       # The SG associated with the primary DB.
  source_security_group_id = aws_security_group.app.id # The EC2 instance for the BFD Pipeline app.
}

# NACL to manage communication between the BFD and RDA environments
# By default this NACL won't add any additonal security, however if for any reason
# we need to shut off communication between the two VPCs, one could simply flip the
# action(s) defined in these rules from "ALLOW" to "DENY"
resource "aws_network_acl" "rda" {
  count = var.mpm_rda_cidr_block ? 1 : 0

  vpc_id      = var.env_config.vpc_id
  tags        = merge({ Name = "bfd-${var.env_config.env}-etl-app" }, var.env_config.tags)

  ingress {
    from_port   = 0
    to_port     = 0
    rule_no     = 100
    action      = "allow"
    protocol    = "all"
    cidr_block  = var.mpm_rda_cidr_block
  }

  egress {
    from_port   = 0
    to_port     = 0
    rule_no     = 200
    action      = "allow"
    protocol    = "all"
    cidr_block  = var.mpm_rda_cidr_block
  }
}

# IAM policy and role to allow the BFD Pipeline read-write access to ETL bucket.
#
resource "aws_iam_policy" "bfd_pipeline_rif" {
  name        = "bfd-${var.env_config.env}-pipeline-rw-s3-rif"
  description = "Allow the BFD Pipeline application to read-write the S3 bucket with the RIF in it."

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BFDPipelineRWS3RIFKMS",
      "Action": [
        "kms:Encrypt",
        "kms:Decrypt",
        "kms:ReEncrypt*",
        "kms:GenerateDataKey*",
        "kms:DescribeKey"
      ],
      "Effect": "Allow",
      "Resource": ["${data.aws_kms_key.master_key.arn}"]
    },
    {
      "Sid": "BFDPipelineRWS3RIFListBucket",
      "Action": ["s3:ListBucket"],
      "Effect": "Allow",
      "Resource": ["${data.aws_s3_bucket.rif.arn}"]
    },
    {
      "Sid": "BFDPipelineRWS3RIFReadWriteObjects",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation",
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Effect": "Allow",
      "Resource": ["${data.aws_s3_bucket.rif.arn}/*"]
    }
  ]
}
EOF
}

module "iam_profile_bfd_pipeline" {
  source = "../iam"

  env_config = var.env_config
  name       = "bfd_pipeline"
}

resource "aws_iam_role_policy_attachment" "bfd_pipeline_rif" {
  role       = module.iam_profile_bfd_pipeline.role
  policy_arn = aws_iam_policy.bfd_pipeline_rif.arn
}

# Give the BFD Pipeline app read access to the Ansible Vault PW.
#
data "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  arn = "arn:aws:iam::${var.launch_config.account_id}:policy/bfd-ansible-vault-pw-ro-s3"
}

resource "aws_iam_role_policy_attachment" "bfd_pipeline_iam_ansible_vault_pw_ro_s3" {
  role       = module.iam_profile_bfd_pipeline.role
  policy_arn = data.aws_iam_policy.ansible_vault_pw_ro_s3.arn
}

# Attach the amazon manged AmazonElasticFileSystemReadOnlyAccess policy to the instance role
# This is needed to query EFT EFS file systems
resource "aws_iam_role_policy_attachment" "aws_efs_read_only_access" {
  role       = module.iam_profile_bfd_pipeline.role
  policy_arn = "arn:aws:iam::aws:policy/AmazonElasticFileSystemReadOnlyAccess"
}

# Policy to allow pipeline instance role to make AWS api calls via the cli
# - DescribeAvailabilityZones is needed to query/mount EFT EFS file systems
resource "aws_iam_policy" "aws_cli" {
  name        = "bfd-pipeline-${var.env_config.env}-cli"
  description = "AWS cli privileges for the BFD Pipeline instance role."

  policy = <<EOF
{
   "Version": "2012-10-17",
   "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeAvailabilityZones"
      ],
      "Resource": "*"
    }
   ]
}
EOF
}
resource "aws_iam_role_policy_attachment" "aws_cli" {
  role       = module.iam_profile_bfd_pipeline.role
  policy_arn = aws_iam_policy.aws_cli.arn
}

# EC2 Instance to run the BFD Pipeline app.
#
module "ec2_instance" {
  source = "../ec2"

  env_config = var.env_config
  role       = "etl"
  layer      = "data"
  az         = "us-east-1b" # Same as the master db

  launch_config = {
    # instance_type must support NVMe EBS volumes: https://github.com/CMSgov/beneficiary-fhir-data/pull/110
    instance_type = local.is_prod ? "m5.4xlarge" : "m5.xlarge" # Use reserve instances. Use 4x only in prod. 
    volume_size   = 1000                                       # GB                                   # Make sure we have nough space to download RIF files
    ami_id        = var.launch_config.ami_id

    key_name      = var.launch_config.ssh_key_name
    profile       = module.iam_profile_bfd_pipeline.profile
    user_data_tpl = "pipeline_server.tpl"
    git_branch    = var.launch_config.git_branch
    git_commit    = var.launch_config.git_commit
  }

  mgmt_config = {
    vpn_sg    = var.mgmt_config.vpn_sg
    tool_sg   = var.mgmt_config.tool_sg
    remote_sg = var.mgmt_config.remote_sg
    ci_cidrs  = var.mgmt_config.ci_cidrs
  }

  sg_ids = [aws_security_group.app.id]

  # Ensure that the DB is accessible before the BFD Pipeline is launched.
  ec2_depends_on_1 = "aws_security_group_rule.allow_db_primary_access"
}
