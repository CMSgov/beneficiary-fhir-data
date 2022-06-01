provider "aws" {
  region = "us-east-1"
}

locals {
  # NOTE: known environments object is hopeful temporary, expedient work-around until parameter implementation
  environments = {
    test = {
      rds_cluster_identifier = "bfd-1652-v70-pre-synthea-load" # TODO: Temporary. To be removed after BFD-1746
    }
    prod-sbx = {}
    prod     = {}
  }

  common_tags = {
    Environment = local.env
    Layer       = local.layer
    Name        = "bfd-${local.env}-${local.service}"
    application = "bfd"
    business    = "oeda"
    role        = local.service
    stack       = local.env
  }

  account_id          = data.aws_caller_identity.current.account_id
  env                 = terraform.workspace
  layer               = "data"
  rds_writer_endpoint = data.external.rds.result["Endpoint"]
  service             = "migrator"

  # TODO: consider how these could be more flexible/automatic in forthcoming ephemeral environments
  key_pair = "bfd-${local.env}"
  vpc_name = "bfd-${local.env}-vpc"

  # TODO: simplify post-parameter store adoption
  ami_id                                      = var.ami_id
  instance_type                               = var.instance_type_override != null ? var.instance_type_override : "m5.large"
  migrator_instance_count                     = var.create_migrator_instance ? 1 : 0
  queue_name                                  = var.sqs_queue_name_override != null ? var.sqs_queue_name_override : "${local.env}-db-migrator"
  security_group_ids                          = concat(var.security_group_ids_extra, [data.aws_security_group.vpn.id])
  volume_size                                 = var.volume_size_override != null ? var.volume_size_override : 100
  rds_cluster_identifier                      = var.rds_cluster_identifier_override != null ? var.rds_cluster_identifier_override : lookup(local.environments[local.env], "rds_cluster_identifier", "bfd-${local.env}-aurora-cluster")
  migrator_monitor_enabled                    = var.migrator_monitor_enabled_override != null ? var.migrator_monitor_enabled_override : true
  migrator_monitor_heartbeat_interval_seconds = var.migrator_monitor_heartbeat_interval_seconds_override != null ? var.migrator_monitor_heartbeat_interval_seconds_override : 300
}

resource "aws_sqs_queue" "this" {
  name = local.queue_name
}

resource "aws_instance" "this" {
  count                = local.migrator_instance_count
  ami                  = local.ami_id
  instance_type        = local.instance_type
  key_name             = local.key_pair
  iam_instance_profile = aws_iam_instance_profile.this.name

  availability_zone           = data.external.rds.result["WriterAZ"]
  tags                        = local.common_tags
  monitoring                  = true
  associate_public_ip_address = false
  ebs_optimized               = true

  vpc_security_group_ids = concat(local.security_group_ids, [aws_security_group.this.id])
  subnet_id              = data.aws_subnet.main.id

  root_block_device {
    tags                  = merge(local.common_tags, { snapshot = "true" })
    volume_type           = "gp2"
    volume_size           = local.volume_size
    delete_on_termination = true
    encrypted             = true
    kms_key_id            = data.aws_kms_key.main.arn
  }

  user_data = templatefile("${path.module}/user-data.tftpl", {
    account_id                                  = local.account_id
    db_migrator_db_url                          = "jdbc:postgresql://${local.rds_writer_endpoint}:5432/fhirdb"
    env                                         = local.env
    git_branch_name                             = var.git_branch_name
    migrator_monitor_enabled                    = local.migrator_monitor_enabled
    migrator_monitor_heartbeat_interval_seconds = local.migrator_monitor_heartbeat_interval_seconds
  })
}
