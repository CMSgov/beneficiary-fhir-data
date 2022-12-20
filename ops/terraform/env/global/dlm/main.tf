terraform {
  required_version = "> 0.12.30, < 0.13"
}

provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

##
# Global DLM policy that takes snapshots of EBS volumes
module "dlm" {
  source = "../../../modules/resources/dlm"
  retain = 21 # days
  time   = "23:30"
}
