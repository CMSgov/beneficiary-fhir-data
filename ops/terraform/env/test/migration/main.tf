terraform {
  required_version = "> 0.12.30, < 0.13" 
}

provider "aws" {
  version = "~> 2.25"
  region  = "us-east-1"
}

module "migration" {
  source = "../../../modules/migration"

  env_config = {
    env  = "test"
    tags = { application = "bfd", business = "oeda", stack = "test", Environment = "test" }
  }

  # These values control the CCS and HealthApt DNS record weight for each partner
  # A value of:
  #   100 - 100% CCS, 0% HealthApt
  #    50 - 50% CCS, 50% HealthApt
  #     0 - 0 CCS, 100% HealthApt
  #
  bb   = 100
  bcda = 100
  dpc  = 100
  mct  = 100
}
