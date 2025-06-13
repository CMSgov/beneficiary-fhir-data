terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.9"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/02-insights"
}

locals {
  service = "insights"
  partners = ["bcda", "bfd", "bb2", "ab2d"]

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  latest_bfd_release       = module.terraservice.latest_bfd_release
  ssm_config               = module.terraservice.ssm_config
  env_key_alias            = module.terraservice.env_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  azs                      = keys(module.terraservice.default_azs)
}

# Temporary placeholder for Greenfield Insights Bucket structure to enable applying server-insights.
# Legacy has a single Bucket, but Greenfield will have a bucket per-environment, per-consumer
# TODO: Make this Terraservice more useful
module "bucket_insights_bfd" {
  count  = !var.greenfield ? 0 : 1
  source = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_prefix      = "bfd-${local.env}-insights-bfd"
  force_destroy      = local.is_ephemeral_env

  ssm_param_name = "/bfd/${local.env}/${local.service}/nonsensitive/bucket"
}

resource "aws_s3_object" "env_prefixes" {
  for_each = merge(
    {
      for partner in local.partners: "${partner}-streams" => {
        key = "${partner}/streams/"
      }
    },
    {
      for partner in local.partners: "${parner}-results" => {
        key = "${partner}/results/"
      }
    }
  )

  bucket = module.bucket_insights_bfd.bucket.name
  key = each.value.key
  content = ""
  content_type = "application/octet-stream"
}

resource "aws_athena_workgroup" "this" {
  for_each = toset([local.partners]) 
  name = "${local.env}-${each.key}"

  configuration {
    enforce_workgroup_configuration = true
    publish_cloudwatch_metrics_enabled = true

    result_configuration {
      output_location = "s3://${module.bucket_insights_bfd.bucket.name}/${each.key}/results/"
  
      encryption_configuration {
        encryption_option = "SSE_KMS"
        kms_key_arn = module.terraservice.aws_kms_key.env.arn
      }
    }
  }

  state = "ENABLED"
}

resource "aws_glue_catalog_database" "this" {
  for_each = toset(local.partners)
  name = "${local.env}-${each.key}"
}

resource "aws_glue_crawler" "this" {
  for_each = toset(local.partners)
  name = "bfd-${local.env}-${each.key}-crawler"
  role = aws_iam_role.glue_role.arn
  database_name = aws_glue_catalog_database.this["${local.env}-${each.key}"].name
  #table_prefix 

  s3_target {
   path = "s3://${module.bucket_insights_bfd.bucket.name}/${each.key}/streams/" 
  }

  configuration = jsonencode({
    Version = "1.0",
    CrawlerOutput = {
      Tables = {
        AddOrUpdateBehavior = "MergeNewColumns"
      },
      Partitions = {
        AddOrUpdateBehavior = "InheritFromTable"
      }
    }
  })

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "UPDATE_IN_DATABASE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  schedule = "cron(0/15 * * * ? *)"
}

resource "aws_iam_role" "this" {
  name = "bfd-insights-glue-crawler-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = {
        Service = "glue.amazonaws.com"
      },
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "this" {
  role = aws_iam_role.this
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole"
}



