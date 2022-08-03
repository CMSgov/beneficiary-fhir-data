provider "aws" {
  region = "us-east-1"
}

locals {
  env     = terraform.workspace
  service = "migrator"
  layer   = "data"

  common_tags = {
    Environment = local.env
    Layer       = local.layer
    Name        = "bfd-${local.env}-${local.service}"
    application = "bfd"
    business    = "oeda"
    role        = local.service
    stack       = local.env
  }

  nonsensitive_common_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_common.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values))
  nonsensitive_common_config = { for key, value in local.nonsensitive_common_map : split("/", key)[5] => value }
  nonsensitive_map           = zipmap(data.aws_ssm_parameters_by_path.nonsensitive.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive.values))
  nonsensitive_config        = { for key, value in local.nonsensitive_map : split("/", key)[5] => value }
  sensitive_map              = zipmap(data.aws_ssm_parameters_by_path.sensitive.names, data.aws_ssm_parameters_by_path.sensitive.values)
  sensitive_config           = { for key, value in local.sensitive_map : split("/", key)[5] => value }

  # User Configuration
  sudoers       = split(" ", local.sensitive_config["sudoers"])
  mgmt_users    = zipmap(data.aws_ssm_parameters_by_path.users.names, data.aws_ssm_parameters_by_path.users.values)
  users_pubkeys = { for username, pubkey in local.mgmt_users : split("_", split("/", username)[5])[1] => pubkey }
  ssh_users = { ssh_users = [for username, pubkey in local.users_pubkeys : {
    "username"   = username,
    "public_key" = pubkey,
    "sudoer"     = contains(local.sudoers, username)
  }] }



  # Data source lookups
  kms_key_arn           = data.aws_kms_key.cmk.arn
  kms_key_id            = data.aws_kms_key.cmk.key_id
  vpn_security_group_id = data.aws_security_group.vpn.id
  rds_writer_endpoint   = data.external.rds.result["Endpoint"]
  account_id            = data.aws_caller_identity.current.account_id

  # SSM Lookup
  instance_type          = local.nonsensitive_config["instance_type"]
  key_pair               = local.nonsensitive_common_config["key_pair"]
  kms_key_alias          = local.nonsensitive_common_config["kms_key_alias"]
  queue_name             = local.nonsensitive_config["sqs_queue_name"]
  rds_cluster_identifier = local.nonsensitive_common_config["rds_cluster_identifier"]
  volume_size            = local.nonsensitive_config["volume_size"]
  vpc_name               = local.nonsensitive_common_config["vpc_name"]

  # Deploy Time Configuration
  ami_id                                      = var.ami_id # TODO: Consider storing AMI in SSM
  migrator_instance_count                     = var.create_migrator_instance ? 1 : 0
  migrator_monitor_enabled                    = var.migrator_monitor_enabled_override != null ? var.migrator_monitor_enabled_override : true
  migrator_monitor_heartbeat_interval_seconds = var.migrator_monitor_heartbeat_interval_seconds_override != null ? var.migrator_monitor_heartbeat_interval_seconds_override : 300
}

resource "aws_sqs_queue" "this" {
  name              = local.queue_name
  kms_master_key_id = local.kms_key_id
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

  subnet_id              = data.aws_subnet.main.id
  vpc_security_group_ids = [data.aws_security_group.vpn.id, aws_security_group.this.id]

  root_block_device {
    tags                  = merge(local.common_tags, { snapshot = "true" })
    volume_type           = "gp2"
    volume_size           = local.volume_size
    delete_on_termination = true
    encrypted             = true
    kms_key_id            = local.kms_key_arn
  }

  user_data = templatefile("${path.module}/user-data.tftpl", {
    account_id                                  = local.account_id
    db_migrator_db_url                          = "jdbc:postgresql://${local.rds_writer_endpoint}:5432/fhirdb"
    env                                         = local.env
    git_repo_version                            = var.git_repo_version # TODO: This works for now, but it's probably more appropriate for image to contain ansible configuration
    migrator_monitor_enabled                    = local.migrator_monitor_enabled
    migrator_monitor_heartbeat_interval_seconds = local.migrator_monitor_heartbeat_interval_seconds
    ssh_users                                   = yamlencode(local.ssh_users)
  })
}
