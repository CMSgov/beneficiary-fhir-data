module "terraservice" {
  source = "git::https://github.com/CMSgov/beneficiary-fhir-data.git//ops/terraform/services/_modules/bfd-terraservice?ref=2.181.0"

  environment_name     = terraform.workspace
  relative_module_root = "ops/terraform/services/migrator"
  additional_tags = {
    Layer = local.layer
    Name  = "bfd-${local.env}-${local.service}"
    role  = local.service
  }
}

locals {
  default_tags       = module.terraservice.default_tags
  env                = module.terraservice.env
  latest_bfd_release = module.terraservice.latest_bfd_release
  seed_env           = module.terraservice.seed_env
  cloudtamer_iam_path = "/delegatedadmin/developer/"
  service = "migrator"
  layer   = "data"

  nonsensitive_common_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_common.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values))
  nonsensitive_common_config = { for key, value in local.nonsensitive_common_map : basename(key) => value }
  nonsensitive_map           = zipmap(data.aws_ssm_parameters_by_path.nonsensitive.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive.values))
  nonsensitive_config        = { for key, value in local.nonsensitive_map : basename(key) => value }
  # SSM Lookup
  enterprise_tools_security_group = local.nonsensitive_common_config["enterprise_tools_security_group"]
  instance_type                   = local.nonsensitive_config["instance_type"]
  key_pair                        = local.nonsensitive_common_config["key_pair"]
  kms_key_alias                   = local.nonsensitive_common_config["kms_key_alias"]
  kms_config_key_alias            = local.nonsensitive_common_config["kms_config_key_alias"]
  queue_name                      = local.nonsensitive_config["sqs_queue_name"]
  rds_cluster_identifier          = local.nonsensitive_common_config["rds_cluster_identifier"]
  rds_writer_endpoint             = data.external.rds.result["Endpoint"]
  vpc_name                        = local.nonsensitive_common_config["vpc_name"]

  volume_iops       = local.nonsensitive_config["volume_iops"]
  volume_size       = local.nonsensitive_config["volume_size"]
  volume_throughput = local.nonsensitive_config["volume_throughput"]
  volume_type       = local.nonsensitive_config["volume_type"]

  # Data source lookups
  mgmt_kms_config_key_arns = flatten(
    [
      for v in data.aws_kms_key.mgmt_config_cmk.multi_region_configuration :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
  kms_config_key_arns = flatten(
    [
      for v in data.aws_kms_key.config_cmk.multi_region_configuration :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
  kms_key_arn           = data.aws_kms_key.cmk.arn
  kms_key_id            = data.aws_kms_key.cmk.key_id
  vpn_security_group_id = data.aws_security_group.vpn.id
  ent_tools_sg_id       = data.aws_security_group.enterprise_tools.id
  account_id            = data.aws_caller_identity.current.account_id

  # Deploy Time Configuration
  ami_id                  = data.aws_ami.main.image_id
  migrator_instance_count = var.create_migrator_instance ? 1 : 0
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

  metadata_options {
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 1
    http_tokens                 = "required"
  }

  root_block_device {
    tags                  = local.default_tags
    volume_type           = local.volume_type
    volume_size           = local.volume_size
    delete_on_termination = true
    encrypted             = true
    kms_key_id            = local.kms_key_arn
  }

  user_data = templatefile("${path.module}/user-data.tftpl", {
    env             = local.env
    seed_env        = local.seed_env
    writer_endpoint = "jdbc:postgresql://${local.rds_writer_endpoint}:5432/fhirdb"
  })
}
