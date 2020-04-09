terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.25"
  region = "us-east-1"
}

module "stateful" {
  source = "../../../modules/stateful"

  aurora_config = {
    instance_class = "db.r5.xlarge"
    cluster_nodes  = 3
    engine_version = "11.6"
    param_version  = "aurora-postgresql11"
  }

  aurora_node_params = [
    {name = "auto_explain.log_min_duration", value = "6000", apply_on_reboot = false},
    {name = "auto_explain.log_analyze", value = "1", apply_on_reboot = false},
    {name = "auto_explain.log_buffers", value = "1", apply_on_reboot = false},
    {name = "auto_explain.log_timing", value = "1", apply_on_reboot = false},
    {name = "auto_explain.log_nested_statements", value = "1", apply_on_reboot = false},
    {name = "shared_preload_libraries", value = "pg_stat_statements,auto_explain", apply_on_reboot = true},
    {name = "log_min_duration_statement", value = "6000", apply_on_reboot = false},
    {name = "log_temp_files", value = "1", apply_on_reboot = false},
    {name = "log_lock_waits", value = "1", apply_on_reboot = false},
    {name = "log_connections", value = "1", apply_on_reboot = false}
  ]

  db_config = { 
    instance_class    = "db.m5.2xlarge"
    iops              = 1000
    allocated_storage = 2000
  }

  db_params = [
    {name="max_wal_senders", value="15", apply_on_reboot=true},
  ]

  db_import_mode = {
    enabled = false
    maintenance_work_mem = "1048576"
  }

  env_config = {
    env               = "prod-sbx"
    tags              = {application="bfd", business="oeda", stack="prod-sbx", Environment="prod-sbx"}
  }

  victor_ops_url      = var.victor_ops_url

  medicare_opt_out_config = {
    read_roles        = ["arn:aws:iam::755619740999:role/dpc-dev-consent-execution-role"]
    write_roles       = ["arn:aws:iam::755619740999:role/bcda-dev-nfs-instance", "arn:aws:iam::755619740999:role/bcda-test-nfs-instance"]
    admin_users       = ["arn:aws:iam::577373831711:user/DS7H", "arn:aws:iam::577373831711:user/VZG9", "arn:aws:iam::577373831711:user/BYSK"]
  }
}
