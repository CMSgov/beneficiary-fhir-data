module "terraservice" {
  source               = "../../../terraform-modules/bfd/bfd-terraservice"
  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/02-insights-bb2"
}

locals {
  env                 = module.terraservice.env
  insights_bucket_env = var.insights_env != "" ? var.insights_env : local.env
  bucket_param        = "/bfd/${local.insights_bucket_env}/insights/nonsensitive/bfd-insights-${var.project}"
  env_key_alias       = module.terraservice.env_key_alias
  account_id          = data.aws_caller_identity.current.account_id
}

data "aws_caller_identity" "current" {}

/*data "aws_kms_key" "bucket_cmk" {
  key_id = "alias/bfd-insights-bb2-cmk"
}*/

/*data "aws_s3_bucket" "main" {
  bucket = "bfd-insights-bb2-${local.account_id}"
}*/

# Glue crawler
resource "aws_glue_crawler" "glue_crawler" {
  name          = "${var.project}-${var.name}"
  database_name = var.database
  role          = "bfd-insights/bfd-insights-${var.project}-glue-role"
  schedule      = var.glue_crawler_schedule

  s3_target {
    path = "s3://${data.aws_s3_bucket.bucket.id}/databases/events-${var.name}"
  }

  configuration = jsonencode({
    CrawlerOutput = {
      Partitions = {
        AddOrUpdateBehavior = "InheritFromTable"
      }
    }
    Grouping = {
      TableGroupingPolicy     = "CombineCompatibleSchemas"
      TableLevelConfiguration = 4
    }
    Version = 1
  })

  schema_change_policy {
    delete_behavior = "DEPRECATE_IN_DATABASE"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}
