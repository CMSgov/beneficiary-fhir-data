##
# Setup a global DLM policy that takes snapshots of EBS volumes
##

terraform {
  required_version = "> 0.12.30, < 0.13" 
}

provider "aws" {
  version = "~> 2.28"
  region  = "us-east-1"
}

module "dlm" {
  source = "../../../modules/resources/dlm"
  retain = 21 # days
  time   = "23:30"
}
