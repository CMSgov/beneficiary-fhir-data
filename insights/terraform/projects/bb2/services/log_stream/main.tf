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
  region = "us-east-1"

  # Shared lambda name/role (TODO: Split out by env)
  lambda_name = "bb2-kinesis-firehose-cloudwatch-logs-processor-python"
  lambda_role = "bb2-kinesis-firehose-cloudwatch-logs-processor-pyt-role-s0acxwoq"

  firehose_name = "${local.env}-perf-mon"
  glue_crawler_name = "${local.env}-perf-mon"

}

module "lambda" {
  source          = "./modules/lambda"

  name = local.lambda_name
  description = "An Amazon Kinesis Firehose stream processor that extracts individual log events from records sent by Cloudwatch Logs subscription filters."
  role = local.lambda_role
  region = local.region
  application   = local.application
  project       = local.project
}

module "firehose" {
  source          = "./modules/firehose"

  name          = local.firehose_name
  table_name    = "events_${local.env}_perf_mon"
  lambda_name   = local.lambda_name
  project       = local.project
  database      = local.project
  region        = local.region

  firehose_s3_buffer_interval = var.firehose_s3_buffer_interval
  firehose_s3_buffer_size     = var.firehose_s3_buffer_size
}

module "cwl_destination" {
  source = "./modules/cwl_destination"

  bb2_acct      = var.bb2_acct
  firehose_name = "${local.env}-perf-mon"
  project       = local.project
  region        = local.region

}

module "glue_crawler" {
  source = "./modules/glue_crawler"

  name          = local.glue_crawler_name
  project       = local.project
  database      = local.project

  # Get mapped schedule for target env
  glue_crawler_schedule = lookup(var.glue_crawler_schedules, terraform.workspace, "")
}