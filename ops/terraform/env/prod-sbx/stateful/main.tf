terraform {
  required_version = "> 0.12.30, < 0.13" 
}

provider "aws" {
  version = "~> 2.25"
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
    engine_version = "11.6"
    param_version  = "aurora-postgresql11"
  }

  aurora_node_params = [
    { name = "auto_explain.log_min_duration", value = "6000", apply_on_reboot = false },
    { name = "shared_preload_libraries", value = "pg_stat_statements,auto_explain", apply_on_reboot = true },
    { name = "log_min_duration_statement", value = "6000", apply_on_reboot = false },
    { name = "log_connections", value = "1", apply_on_reboot = false },
    { name = "default_statistics_target", value = "1000", apply_on_reboot = false },
    { name = "random_page_cost", value = "1.1", apply_on_reboot = false },
    { name = "work_mem", value = "32768", apply_on_reboot = false }
  ]

  # db_config = {
  #   instance_class    = "db.m5.2xlarge"
  #   iops              = 1000
  #   allocated_storage = 2000
  # }

  # db_params = [
  #   {name="max_wal_senders", value="15", apply_on_reboot=true},
  # ]

  # db_import_mode = {
  #   enabled = false
  #   maintenance_work_mem = "1048576"
  # }

  env_config = {
    env  = "prod-sbx"
    tags = { application = "bfd", business = "oeda", stack = "prod-sbx", Environment = "prod-sbx" }
  }

  victor_ops_url = var.victor_ops_url

  partner_acct_nums = var.partner_acct_nums
  partner_subnets   = var.partner_subnets

  medicare_opt_out_config = var.medicare_opt_out_config
}
