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
    instance_class = "db.r5.12xlarge"

    # With aurora you do not designate primary and replicas. Instead, you simply add RDS Instances and
    # Aurora manages the replication. So if you want 1 writer and 3 readers, you set cluster_nodes to 4
    cluster_nodes  = 4
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

  env_config = {
    env  = "prod"
    tags = { application = "bfd", business = "oeda", stack = "prod", Environment = "prod" }
  }

  victor_ops_url = var.victor_ops_url

  medicare_opt_out_config = {
    # TODO: add read roles for DPC
    read_roles  = ["arn:aws:iam::595094747606:role/Ab2dInstanceRole"]
    write_accts = ["arn:aws:iam::755619740999:root"]
    admin_users = ["arn:aws:iam::577373831711:user/DS7H", "arn:aws:iam::577373831711:user/VZG9"]
  }
}
