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
  lambda_name = "bb2-lambda-create-tables-for-quicksight"
  lambda_role = "bb2-lambda-create-tables-for-quicksight-role-h3p38tic"

}

module "lambda" {
  source = "./modules/lambda"

  name        = local.lambda_name
  description = "Lambda function to create intermediate reporting tables via Athena for usage in QuickSight."
  role        = local.lambda_role
  region      = local.region
  application = local.application
  project     = local.project
}

module "event_bridge" {
  source = "./modules/event_bridge"

  for_each = var.event_bridge_schedules

  name        = "bb2-lambda-create-tables-for-quicksight-${each.key}-nightly-event"
  description = "Event to schedule a nightly execution of the BB2 lambda function (${each.key} with JSON params."
  schedule    = each.value
}
