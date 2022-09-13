terraform {
  required_version = "> 0.12.30, < 0.13"
}

provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

module "stateless" {
  source = "../../../modules/stateless"

  env_config = {
    env  = "prod-sbx"
    tags = { application = "bfd", business = "oeda", stack = "prod-sbx", Environment = "prod-sbx" }
  }

  fhir_ami        = var.fhir_ami
  ssh_key_name    = var.ssh_key_name
  git_branch_name = var.git_branch_name
  git_commit_id   = var.git_commit_id
  is_public       = true

  ## Cloudwatch Dashboard ##
  ## This is where the dashboard params are passed ##
  dashboard_name      = "bfd-server-prod-sbx"
  dashboard_namespace = "bfd-prod-sbx/bfd-server"
}
