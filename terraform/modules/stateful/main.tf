#
# Build the stateful resources for an environment.
#
# This script also builds the associated KMS needed by the stateful and stateless resources
#

locals {
  azs = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=aws_route53_zone.local_zone.id }
  
  db_sgs = [
    aws_security_group.db.id,
    data.aws_security_group.vpn.id,
    data.aws_security_group.tools.id,
    data.aws_security_group.management.id
  ]
  master_db_sgs = [
    aws_security_group.master_db.id,
    data.aws_security_group.vpn.id,
    data.aws_security_group.tools.id,
    data.aws_security_group.management.id
  ]

  cw_period             = 60    # Seconds
  cw_eval_periods       = 3
  cw_disk_queue_depth   = 5
  cw_replica_lag        = 600   # Seconds
  cw_latency            = 0.2   # Seconds
}

# VPC
#
data "aws_vpc" "main" {
  filter {
    name = "tag:Name"
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
    name    = "tag:Layer"
    values  = ["data"] 
  }
}

# Other Security Groups
#
# Find the security group for the Cisco VPN
#
data "aws_security_group" "vpn" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-vpn-private"]
  }
}

# Find the management group
#
data "aws_security_group" "tools" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-enterprise-tools"]
  }
}

# Find the tools group 
#
data "aws_security_group" "management" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-remote-management"]
  }
}

#
# Start to build things
#

# DNS
#
# Build a VPC private local zone for CNAME records
#
resource "aws_route53_zone" "local_zone" {
  name    = "bfd-${var.env_config.env}.local"
  vpc {
    vpc_id = data.aws_vpc.main.id
  }
}

# CloudWatch SNS Topic
#
resource "aws_sns_topic" "cloudwatch_alarms" {
  name          = "bfd-${var.env_config.env}-cloudwatch-alarms"
  display_name  = "BFD Cloudwatch Alarm. Created by Terraform."
  tags          = var.env_config.tags
}

# DB Security group
#
resource "aws_security_group" "db" {
  name        = "bfd-${var.env_config.env}-rds"
  description = "Security group for replica DPC DB"
  vpc_id      = local.env_config.vpc_id
  tags        = merge({Name="bfd-${var.env_config.env}-rds"}, local.env_config.tags)

  # Ingress will come from security group rules defined later
  egress {
    from_port = 0
    protocol  = "-1"
    to_port   = 0

    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "master_db" {
  name        = "bfd-${var.env_config.env}-master-rds"
  description = "Security group for master DPC DB"
  vpc_id      = local.env_config.vpc_id
  tags        = merge({Name="bfd-${var.env_config.env}-master-rds"}, local.env_config.tags)

  # Ingress will come from security group rules defined later
  egress {
    from_port = 0
    protocol  = "-1"
    to_port   = 0

    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group_rule" "allow_master_access" {
  type                      = "ingress"
  from_port                 = 5432
  to_port                   = 5432
  protocol                  = "tcp"

  description               = "Allow every replica db to call the master"
  security_group_id         = aws_security_group.master_db.id  
  source_security_group_id  = aws_security_group.db.id     
}

# Subnet Group
#
resource "aws_db_subnet_group" "db" {
  name            = "bfd-${local.env_config.env}-subnet-group"
  tags            = local.env_config.tags
  subnet_ids      = [for s in data.aws_subnet.data_subnets: s.id]
}

# Master Database
#
module "master" {
  source              = "../resources/rds"
  db_config           = var.db_config
  env_config          = local.env_config
  role                = "master"
  availability_zone   = local.azs[1]
  replicate_source_db = ""
  subnet_group        = aws_db_subnet_group.db.name
  kms_key_id          = data.aws_kms_key.master_key.arn

  vpc_security_group_ids = local.master_db_sgs
}

# Replicas Database 
# 
# No count on modules yet, so do build them one by one
#
module "replica1" {
  source              = "../resources/rds"
  db_config           = var.db_config
  env_config          = local.env_config
  role                = "replica1"
  availability_zone   = local.azs[0]
  replicate_source_db = module.master.identifier
  subnet_group        = aws_db_subnet_group.db.name
  kms_key_id          = data.aws_kms_key.master_key.arn

  vpc_security_group_ids = local.db_sgs
}

module "replica2" {
  source              = "../resources/rds"
  db_config           = var.db_config
  env_config          = local.env_config
  role                = "replica2"
  availability_zone   = local.azs[1]
  replicate_source_db = module.master.identifier
  subnet_group        = aws_db_subnet_group.db.name
  kms_key_id          = data.aws_kms_key.master_key.arn

  vpc_security_group_ids = local.db_sgs
}

module "replica3" {
  source              = "../resources/rds"
  db_config           = var.db_config
  env_config          = local.env_config
  role                = "replica3"
  availability_zone   = local.azs[2]
  replicate_source_db = module.master.identifier
  subnet_group        = aws_db_subnet_group.db.name
  kms_key_id          = data.aws_kms_key.master_key.arn

  vpc_security_group_ids = local.db_sgs
}

# Cloud Watch alarms for each RDS instance
#
module "master_alarms" {
  source              = "../resources/rds_alarms"
  rds_name            = module.master.identifier
  env                 = var.env_config.env
  app                 = "bfd"

  free_storage = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = 50000000000 # Bytes
  }

  write_latency = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_latency
  }

  disk_queue_depth = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_disk_queue_depth
  }

  cloudwatch_notification_arn = aws_sns_topic.cloudwatch_alarms.arn
}

module "replica1_alarms" {
  source              = "../resources/rds_alarms"
  rds_name            = module.replica1.identifier
  env                 = var.env_config.env
  app                 = "bfd"

  read_latency = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_latency
  }

  disk_queue_depth = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_disk_queue_depth
  }

  replica_lag = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_replica_lag
  }

  cloudwatch_notification_arn = aws_sns_topic.cloudwatch_alarms.arn
}

module "replica2_alarms" {
  source              = "../resources/rds_alarms"
  rds_name            = module.replica2.identifier
  env                 = var.env_config.env
  app                 = "bfd"

  read_latency = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_latency
  }

  disk_queue_depth = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_disk_queue_depth
  }

  replica_lag = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_replica_lag
  }

  cloudwatch_notification_arn = aws_sns_topic.cloudwatch_alarms.arn
}

module "replica3_alarms" {
  source              = "../resources/rds_alarms"
  rds_name            = module.replica3.identifier
  env                 = var.env_config.env
  app                 = "bfd"

  read_latency = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_latency
  }

  disk_queue_depth = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_disk_queue_depth
  }

  replica_lag = {
    period            = local.cw_period
    eval_periods      = local.cw_eval_periods
    threshold         = local.cw_replica_lag
  }

  cloudwatch_notification_arn = aws_sns_topic.cloudwatch_alarms.arn
}

# S3 Admin bucket for logs and other adminstrative 
#
module "admin" { 
  source              = "../resources/s3"
  role                = "admin"
  env_config          = local.env_config
  kms_key_id          = data.aws_kms_key.master_key.arn
  acl                 = "log-delivery-write"
}

# S3 bucket for ETL files
#
module "etl" {
  source              = "../resources/s3"
  role                = "etl"
  env_config          = local.env_config
  kms_key_id          = data.aws_kms_key.master_key.arn
  log_bucket          = module.admin.id
}
