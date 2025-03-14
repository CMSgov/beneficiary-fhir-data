data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}

data "aws_kms_key" "config_cmk" {
  key_id = local.kms_config_key_alias
}

# This is a distinct parameter as we need to retrieve the list of partners first so that we know
# which SSM hierarchies to get
data "aws_ssm_parameter" "partners_list_json" {
  for_each = toset(["inbound", "outbound"])

  name            = "/bfd/${local.env}/${local.service}/sensitive/${each.key}/partners_list_json"
  with_decryption = true
}

data "aws_ssm_parameters_by_path" "params" {
  for_each = toset(local.ssm_hierarchies)

  recursive       = true
  path            = each.value
  with_decryption = true
}

data "aws_ecr_repository" "ecr" {
  name = "bfd-mgmt-${local.service}-${local.outbound_lambda_name}-lambda"
}

data "aws_ecr_image" "sftp_outbound_transfer" {
  repository_name = data.aws_ecr_repository.ecr.name
  image_tag       = local.bfd_version
}

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

data "aws_vpc" "this" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

data "aws_subnets" "sftp_outbound_transfer" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.this.id]
  }
  filter {
    name   = "tag:Layer"
    values = [local.layer]
  }
}

data "aws_subnet" "this" {
  for_each = local.subnet_ip_reservations

  vpc_id = local.vpc_id
  filter {
    name   = "tag:Name"
    values = [each.key]
  }
}

data "aws_network_interface" "vpc_endpoint" {
  # This is a bit strange looking, but we cannot use network_interface_ids within a for_each as its
  # value is not determined until _after_ aws_vpc_endpoint.this is applied. Terraform expects the
  # set it iterates upon is known prior to apply, so instead we iterate on a common, known
  # constraint which is the subnets that this service exists in
  count = length(local.available_endpoint_subnets)
  # network_interface_ids is a set by default, and sets have no index in Terraform, so we need to
  # convert it to a list. Then, to make it consistent between runs, we sort it
  id = sort(tolist(aws_vpc_endpoint.this.network_interface_ids))[count.index]
}

data "aws_vpc_endpoint_service" "transfer_server" {
  service_name = "com.amazonaws.us-east-1.transfer.server"
}

data "aws_ssm_parameter" "zone_name" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_${local.inbound_r53_hosted_zone}_domain"
  with_decryption = true
}

data "aws_ssm_parameter" "zone_is_private" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_${local.inbound_r53_hosted_zone}_is_private"
  with_decryption = true
}

data "aws_route53_zone" "this" {
  name         = nonsensitive(data.aws_ssm_parameter.zone_name.value)
  private_zone = nonsensitive(data.aws_ssm_parameter.zone_is_private.value)
  tags = {
    "ConfigId" = local.inbound_r53_hosted_zone
  }
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
