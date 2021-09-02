terraform {
  required_version = "> 0.12.30, < 0.13"
}

provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

module "stateful" {
  source = "../../../modules/stateful"

  # feature toggles
  module_features = {
    beta_reader = false
  }

  aurora_config = {
    instance_class = "db.r5.2xlarge"
    cluster_nodes  = 3
    engine_version = "12.6"
    param_version  = "aurora-postgresql12"
  }

  aurora_node_params = [
    { name = "auto_explain.log_min_duration", value = "1000", apply_on_reboot = false },
    { name = "auto_explain.log_verbose", value = "1", apply_on_reboot = false },
    { name = "auto_explain.log_nested_statements", value = "1", apply_on_reboot = false },
    { name = "pg_stat_statements.max", value = "5000", apply_on_reboot = true },
    { name = "pg_stat_statements.track", value = "top", apply_on_reboot = false },
    { name = "shared_preload_libraries", value = "pg_stat_statements,auto_explain", apply_on_reboot = true },
    { name = "log_min_duration_statement", value = "1000", apply_on_reboot = false },
    { name = "log_connections", value = "1", apply_on_reboot = false },
    { name = "default_statistics_target", value = "1000", apply_on_reboot = false },
    { name = "random_page_cost", value = "1.1", apply_on_reboot = false },
    { name = "work_mem", value = "32768", apply_on_reboot = false }
  ]

  env_config = {
    env  = "test"
    tags = { application = "bfd", business = "oeda", stack = "test", Environment = "test" }
  }

  victor_ops_url = var.victor_ops_url

  partner_acct_nums = var.partner_acct_nums
  partner_subnets   = var.partner_subnets

  medicare_opt_out_config = var.medicare_opt_out_config
}
