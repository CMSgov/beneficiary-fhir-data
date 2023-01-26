locals {
  env = "test"
  default_tags = {
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Environment    = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/env/${local.env}/stateless"
  }
}

module "stateless" {
  source = "../../../modules/stateless"

  env_config = {
    env  = local.env
    tags = local.default_tags
  }

  fhir_ami        = var.fhir_ami
  ssh_key_name    = var.ssh_key_name
  git_branch_name = var.git_branch_name
  git_commit_id   = var.git_commit_id

  ## Cloudwatch Dashboard ##
  ## This is where the dashboard params are passed ##
  dashboard_name      = "bfd-${local.env}-server"
  dashboard_namespace = "bfd-${local.env}/bfd-server"
}
