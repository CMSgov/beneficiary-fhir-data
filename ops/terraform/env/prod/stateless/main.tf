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
    env  = "prod"
    tags = { application = "bfd", business = "oeda", stack = "prod", Environment = "prod" }
  }

  fhir_ami        = var.fhir_ami
  ssh_key_name    = var.ssh_key_name
  git_branch_name = var.git_branch_name
  git_commit_id   = var.git_commit_id

  ## Cloudwatch Dashboard ##
  ## This is where the dashboard params are passed ##
  dashboard_name      = "bfd-server-prod"
  dashboard_namespace = "bfd-prod/bfd-server"
}
