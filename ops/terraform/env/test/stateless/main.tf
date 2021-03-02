terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  # FIXME BFD-211: Revert once this is fixed: https://github.com/terraform-providers/terraform-provider-aws/issues/13236
  # version = "~> 2.25"
  version = "<= 2.60.0"
  region  = "us-east-1"
}

module "stateless" {
  source = "../../../modules/stateless"

  env_config = {
    env  = "test"
    tags = { application = "bfd", business = "oeda", stack = "test", Environment = "test" }
  }

  fhir_ami        = var.fhir_ami
  etl_ami         = var.etl_ami
  ssh_key_name    = var.ssh_key_name
  git_branch_name = var.git_branch_name
  git_commit_id   = var.git_commit_id
}
