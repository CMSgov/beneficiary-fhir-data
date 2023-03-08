locals {
  env     = terraform.workspace
  service = "migrator"
  layer   = "data"

  default_tags = {
    Environment    = local.env
    Layer          = local.layer
    Name           = "bfd-${local.env}-${local.service}"
    application    = "bfd"
    business       = "oeda"
    role           = local.service
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/migrator"
  }

  nonsensitive_common_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_common.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values))
  nonsensitive_common_config = { for key, value in local.nonsensitive_common_map : split("/", key)[5] => value }
  nonsensitive_map           = zipmap(data.aws_ssm_parameters_by_path.nonsensitive.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive.values))
  nonsensitive_config        = { for key, value in local.nonsensitive_map : split("/", key)[5] => value }

  # SSM Lookup
  enterprise_tools_security_group = local.nonsensitive_common_config["enterprise_tools_security_group"]
  instance_type                   = local.nonsensitive_config["instance_type"]
  key_pair                        = local.nonsensitive_common_config["key_pair"]
  kms_key_alias                   = local.nonsensitive_common_config["kms_key_alias"]
  queue_name                      = local.nonsensitive_config["sqs_queue_name"]
  rds_cluster_identifier          = local.nonsensitive_common_config["rds_cluster_identifier"]
  volume_size                     = local.nonsensitive_config["volume_size"]
  vpc_name                        = local.nonsensitive_common_config["vpc_name"]

  # Data source lookups
  mgmt_kms_key_arn      = data.aws_kms_key.mgmt_cmk.arn
  kms_key_arn           = data.aws_kms_key.cmk.arn
  kms_key_id            = data.aws_kms_key.cmk.key_id
  vpn_security_group_id = data.aws_security_group.vpn.id
  ent_tools_sg_id       = data.aws_security_group.enterprise_tools.id
  rds_writer_endpoint   = data.external.rds.result["Endpoint"]
  account_id            = data.aws_caller_identity.current.account_id

  # Deploy Time Configuration
  ami_id                                      = data.aws_ami.main.image_id
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
  monitoring                  = true
  associate_public_ip_address = false
  ebs_optimized               = true

  subnet_id              = data.aws_subnet.main.id
  vpc_security_group_ids = [local.vpn_security_group_id, aws_security_group.this[0].id, local.ent_tools_sg_id]

  root_block_device {
    tags                  = merge(local.default_tags, { snapshot = "true" }) # TODO: Consider removing the tag from migrator instances
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
    migrator_monitor_enabled                    = local.migrator_monitor_enabled
    migrator_monitor_heartbeat_interval_seconds = local.migrator_monitor_heartbeat_interval_seconds
  })
}
