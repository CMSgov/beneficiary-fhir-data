locals {
  tags = {
    business    = "OEDA"
    application = local.application
    project     = local.project
    Environment = local.env
    stack       = "${local.application}-${local.project}-${local.env}"
  }
  application = "bfd-insights"
  project     = "bb2"
  env         = terraform.workspace
  region      = "us-east-1"

  # Shared lambda name/role (TODO: Split out by env)
  lambda_name = "bb2-kinesis-firehose-cloudwatch-logs-processor-python"
  lambda_role = "bb2-kinesis-firehose-cloudwatch-logs-processor-pyt-role-s0acxwoq"

  yaml = data.external.yaml.result
  kms_key_alias = "alias/bfd-insights-bb2-cmk"
  kms_key_id = data.aws_kms_key.cmk.arn
}

module "lambda" {
  source = "./modules/lambda"

  name        = local.lambda_name
  description = "An Amazon Kinesis Firehose stream processor that extracts individual log events from records sent by Cloudwatch Logs subscription filters."
  role        = local.lambda_role
  region      = local.region
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}

data "external" "yaml" {
  program = ["${path.module}/scripts/tf-decrypt-shim.sh"]
  query = {
    seed_env = local.env
    env         = local.env
    kms_key_alias = local.kms_key_alias
  }
}
