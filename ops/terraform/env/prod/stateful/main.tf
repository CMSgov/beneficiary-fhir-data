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
    beta_reader = true
  }

  aurora_config = {
    instance_class = "db.r5.12xlarge"

    # With aurora you do not designate primary and replicas. Instead, you simply add RDS Instances and
    # Aurora manages the replication. So if you want 1 writer and 3 readers, you set cluster_nodes to 4
    cluster_nodes  = 4
    engine_version = "11.9"
    param_version  = "aurora-postgresql11"
  }

  aurora_node_params = [
    { name = "auto_explain.log_min_duration", value = "1000", apply_on_reboot = false },
    { name = "auto_explain.log_verbose", value = true, apply_on_reboot = false },
    { name = "auto_explain.log_nested_statements", value = true, apply_on_reboot = false },
    { name = "pg_stat_statements.max", value = "2000", apply_on_reboot = true },
    { name = "pg_stat_statements.track", value = "all", apply_on_reboot = false },
    { name = "shared_preload_libraries", value = "pg_stat_statements,auto_explain", apply_on_reboot = true },
    { name = "log_min_duration_statement", value = "1000", apply_on_reboot = false },
    { name = "log_connections", value = "1", apply_on_reboot = false },
    { name = "default_statistics_target", value = "1000", apply_on_reboot = false },
    { name = "random_page_cost", value = "1.1", apply_on_reboot = false },
    { name = "work_mem", value = "32768", apply_on_reboot = false }
  ]

  env_config = {
    env  = "prod"
    tags = { application = "bfd", business = "oeda", stack = "prod", Environment = "prod" }
  }

  victor_ops_url = var.victor_ops_url

  partner_acct_nums = var.partner_acct_nums
  partner_subnets   = var.partner_subnets

  medicare_opt_out_config = var.medicare_opt_out_config
}
