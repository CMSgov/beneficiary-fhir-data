# BFD Insights: BFD Project: Common Terraform
#
# Set up a common S3 bucket, S3 inventory, Athena workgroup, and IAM resources

locals {
  tags           = { business = "OEDA", application = "bfd-insights", project = "bfd" }
  database       = "bfd"
  project        = "bfd"
  region         = "us-east-1"
  s3_bucket_name = "bfd-insights-${local.project}-app-logs"

  # TODO: Replace when/if this is merged into main Terraform
  # Used for generating S3 event notifications for server-regression-glue-trigger
  # lambda in s3.tf/data-sources.tf
  envs = toset(["test", "prod-sbx", "prod"])
}

# Creates an S3 bucket named "bfd-insights-bfd-${data.aws_caller_identity.current.account_id}"
module "bucket" {
  source      = "../../../modules/bucket"
  name        = local.database
  sensitivity = "high"
  tags        = local.tags
  full_groups = [] # prevent bucket module from attempting to attach policy
}


#NOTE: The following data sources are included with a disabled aws s3 inventory for completeness
#S3 Bucket Inventory resources should be supported by any AWS S3 bucket child module we provision
#in the future.
data "aws_kms_key" "cmk" {
  for_each = local.envs
  key_id   = "alias/bfd-${each.value}-cmk"
}

data "aws_s3_bucket" "admin" {
  for_each = local.envs
  bucket   = "bfd-${each.value}-admin-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_inventory" "bucket" {
  for_each = local.envs
  bucket   = module.bucket.id
  name     = each.value

  included_object_versions = "Current"

  schedule {
    frequency = "Daily"
  }

  optional_fields = [
    "BucketKeyStatus",
    "ETag",
    "EncryptionStatus",
    "LastModifiedDate",
  ]

  enabled = false

  destination {
    bucket {
      account_id = data.aws_caller_identity.current.account_id
      format     = "CSV"
      bucket_arn = data.aws_s3_bucket.admin[each.value].arn
      prefix     = "s3-inventories"
      encryption {
        sse_kms {
          key_id = data.aws_kms_key.cmk[each.value].arn
        }
      }
    }
  }

  filter {
    prefix = "databases/bfd-insights-bfd-${each.value}/"
  }
}

# Creates Athena workgroup named "bfd"
module "workgroup" {
  source     = "../../../modules/workgroup"
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  name       = local.database
  tags       = local.tags
}

# As of right now, this doesn't create any Glue resources, but it does create some IAM resources:
#   Glue role: bfd-insights-bfd-glue-role
module "glue_jobs" {
  source  = "../../../modules/jobs"
  project = local.project
  tags    = local.tags

  buckets = [
    { bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn },

    # CMK is not used on the App-Logs bucket, but it's a required field. By including the same one
    # twice, no additional permissions are created.
    { bucket = aws_s3_bucket.bfd-insights-bfd-app-logs.arn, cmk = module.bucket.bucket_cmk_arn }
  ]
}

# Classifier for the CloudWatch Exports Crawler
resource "aws_glue_classifier" "cw-export" {
  name = "bfd-insights-bfd-cw-export"

  grok_classifier {
    classification = "cw-history"
    grok_pattern   = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
  }
}

# Crawler to create table/partitions on historical CloudWatch exports
resource "aws_glue_crawler" "cw-export" {
  name          = "bfd_cw_export"
  description   = "Crawler for BFD cloudwatch exports"
  database_name = "bfd_cw_export"
  # role          = "arn:aws:iam::${data.aws_caller_identity.current}:role/bfd-insights/bfd-insights-bfd-glue-role"
  role = "bfd-insights/bfd-insights-bfd-glue-role"

  classifiers = [
    aws_glue_classifier.cw-export.name
  ]

  s3_target {
    path = "s3://${aws_s3_bucket.bfd-insights-bfd-app-logs.bucket}/export/prod"
  }

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  schema_change_policy {
    delete_behavior = "DEPRECATE_IN_DATABASE"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}
