locals {
  tags         = { business = "OEDA", application = "bfd-insights", project = "bfd" }
  database     = "bfd"
  project      = "bfd"
  table        = "api-requests"
  full_name    = "${local.project}-${local.database}-${local.table}"
  account_id   = data.aws_caller_identity.current.account_id
  region       = "us-east-1"
  # External resources not in this Terraform module
  external = {
    s3_insights_arn        = "arn:aws:s3:::bfd-insights-bfd-577373831711"
    kms_arn                = "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea"
    insights_glue_role     = "bfd-insights/bfd-insights-bfd-glue-role"
    insights_glue_role_arn = "arn:aws:iam::577373831711:role/bfd-insights/bfd-insights-bfd-glue-role"
    s3_glue_assets_bucket  = "aws-glue-assets-577373831711-us-east-1"
  }
}

module "bucket" {
  source      = "../../../modules/bucket"
  name        = local.database
  sensitivity = "high"
  tags        = local.tags
  full_groups = [] # prevent bucket module from attempting to attach policy
}

# TODO: Are we using this module anywhere?
module "database" {
  source     = "../../../modules/database"
  database   = local.database
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  tags       = local.tags
}

# TODO: Are we using this module anywhere?
module "workgroup" {
  source     = "../../../modules/workgroup"
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  name       = local.database
  tags       = local.tags
}

# TODO: Are we using this module anywhere?
module "glue_jobs" {
  source  = "../../../modules/jobs"
  project = local.project
  tags    = local.tags

  buckets = [
    { bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn }
  ]
}
