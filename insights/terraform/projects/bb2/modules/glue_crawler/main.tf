data "aws_caller_identity" "current" {}

data "aws_kms_key" "bucket_cmk" {
  key_id = "alias/bfd-insights-bb2-cmk"
}

data "aws_s3_bucket" "main" {
  bucket = "bfd-insights-bb2-${local.account_id}"
}

locals {
  account_id = data.aws_caller_identity.current.account_id
}

# Glue crawler
resource "aws_glue_crawler" "glue_crawler" {
  name          = "${var.project}-${var.firehose_name}"
  database_name = var.database
  role          = "bfd-insights/bfd-insights-${var.project}-glue-role"
  s3_target {
    path = "s3://${data.aws_s3_bucket.main.id}/databases/${var.project}/events-${var.firehose_name}"
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