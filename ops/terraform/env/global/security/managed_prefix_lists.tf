terraform {
  required_version = "> 0.12.30, < 0.13"
}

provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

##
# Globally managed prefix lists that we can reference in terraform, security groups, nacl's, etc.
##

# Cloudbees core jenkins subnet.
module "cbc_jenkins" {
  source = "../../../modules/resources/prefix_list"
  name = "bfd-cbc-jenkins"
  max_entries = 1
  entries = var.cbc_jenkins_prefix_list
  tags = {"application"="bfd", "business"="oeda"}
  address_family = "IPv4"
}
