# BFD Insights: BFD Project: Common Terraform
#
# Set up a common S3 bucket, Athena workgroup, and IAM resources

locals {
  tags           = { business = "OEDA", application = "bfd-insights", project = "bfd" }
  database       = "bfd"
  project        = "bfd"
  region         = "us-east-1"
  s3_bucket_name = "bfd-insights-${local.project}-app-logs"
}

# Creates an S3 bucket named "bfd-insights-bfd-${data.aws_caller_identity.current.account_id}"
module "bucket" {
  source      = "../../../modules/bucket"
  name        = local.database
  sensitivity = "high"
  tags        = local.tags
  full_groups = [] # prevent bucket module from attempting to attach policy
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
    { bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn }
  ]
}
