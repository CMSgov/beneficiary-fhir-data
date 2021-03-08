terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.25"
  region  = "us-east-1"
}

module "stateful" {
  source = "../../../modules/stateful"

  aurora_config = {
    instance_class = "db.r5.12xlarge"
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
  #   instance_class    = "db.r5.24xlarge"
  #   iops              = 16000
  #   allocated_storage = 16000
  # }

  # db_params = [
  #   {name="auto_explain.log_min_duration", value="6000", apply_on_reboot=false},
  #   {name="effective_io_concurrency", value="300", apply_on_reboot=false},
  #   {name="default_statistics_target", value="1000", apply_on_reboot=false},
  #   {name="max_worker_processes", value="96", apply_on_reboot=true},
  #   {name="max_wal_senders", value="15", apply_on_reboot=true},
  #   {name="max_parallel_workers_per_gather", value="48", apply_on_reboot=false},
  #   {name="random_page_cost", value="1", apply_on_reboot=false},
  #   {name="temp_buffers", value="8192", apply_on_reboot=false},
  #   {name="work_mem", value="32768", apply_on_reboot=false}
  # ]

  # db_import_mode = {
  #   enabled = false
  #   maintenance_work_mem = "4194304"
  # }

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

  # BCDA EFT EFS vars
  bcda_acct_num = var.bcda_acct_num
  bcda_subnets  = var.bcda_subnets
}
