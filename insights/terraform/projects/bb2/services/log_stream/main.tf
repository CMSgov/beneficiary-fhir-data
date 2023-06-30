locals {
  tags = {
    business = "OEDA"
    application = local.application
    project = local.project
    Environment = local.env
    stack = "${local.application}-${local.project}-${local.env}"
  }
  application = "bfd-insights"
  project = "bb2"
  env = terraform.workspace

}

module "firehose" {
  source          = "./modules/firehose"

  firehose_name = "${local.env}-perf-mon"
  table_name    = "events_${local.env}_perf_mon"
  project       = local.project
  database      = local.project

  firehose_s3_buffer_interval = var.firehose_s3_buffer_interval
  firehose_s3_buffer_size     = var.firehose_s3_buffer_size
}

module "cwl_destination" {
  source = "./modules/cwl_destination"

  bb2_acct      = var.bb2_acct
  firehose_name = "${local.env}-perf-mon"
  project       = local.project

}

module "glue_crawler" {
  source = "./modules/glue_crawler"

  firehose_name = "${local.env}-perf-mon"
  project       = local.project
  database      = local.project

  # Get mapped schedule for target env
  glue_crawler_schedule = lookup(var.glue_crawler_schedules, terraform.workspace, "")
}
