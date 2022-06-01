locals {
  tags         = { business = "OEDA", application = "bfd-insights", project = "bfd" }
  database     = "bfd"
  project      = "bfd"
  table        = "api-requests"
  table_name   = "test-${local.table}"
  full_name    = "bfd-insights-${local.database}-${local.table}"
  account_id   = data.aws_caller_identity.current.account_id
  region       = "us-east-1"
  external = {
    s3_insights_arn        = "arn:aws:s3:::bfd-insights-bfd-577373831711"
    kms_arn                = "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea"
    insights_glue_role     = "bfd-insights/bfd-insights-bfd-glue-role"
    insights_glue_role_arn = "arn:aws:iam::577373831711:role/bfd-insights/bfd-insights-bfd-glue-role"
    s3_glue_assets_bucket  = "aws-glue-assets-577373831711-us-east-1"
  }
  environments = toset( [ "prod-sbx" ] )
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

data "archive_file" "zip_the_python_code" {
  type        = "zip"
  source_dir  = "${path.module}/lambda_src/"
  output_path = "${path.module}/lambda_src/bfd-cw-to-flattened-json.zip"
}

resource "aws_lambda_function" "bfd-cw-to-flattened-json" {
  architectures = [
    "x86_64",
  ]
  description                    = "Extracts and flattens JSON messages from CloudWatch log subscriptions."
  function_name                  = "bfd-cw-to-flattened-json"
  filename                       = data.archive_file.zip_the_python_code.output_path
  source_code_hash               = filebase64sha256("${path.module}/lambda_src/bfd-cw-to-flattened-json.zip")
  handler                        = "bfd-cw-to-flattened-json.lambda_handler"
  layers                         = []
  memory_size                    = 128
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = aws_iam_role.bfd-transform-role-rlenc44a.arn
  runtime                        = "python3.8"
  tags = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor-python"
  }
  tags_all = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor-python"
  }
  timeout = 300

  ephemeral_storage {
    size = 512
  }

  timeouts {}

  tracing_config {
    mode = "PassThrough"
  }
}

resource "aws_glue_crawler" "bfd-test-history-crawler" {
  classifiers = [
    "test_historicals_local",
  ]
  database_name = "bfd"
  name          = "bfd-test-history-crawler"
  role          = local.external.insights_glue_role
  tags          = {}
  tags_all      = {}

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  s3_target {
    exclusions = []
    path       = "s3://${aws_s3_bucket.bfd-insights-bfd-app-logs.bucket}/history/test_api_history"
  }

  schema_change_policy {
    delete_behavior = "DEPRECATE_IN_DATABASE"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}

resource "aws_glue_classifier" "test_historicals_local" {
  name = "test_historicals_local"

  grok_classifier {
    classification = "cw-history"
    grok_pattern   = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
  }
}

resource "aws_s3_bucket" "bfd-insights-bfd-app-logs" {
  bucket              = "bfd-insights-bfd-app-logs"
  hosted_zone_id      = "Z3AQBSTGFYJSTF"
  object_lock_enabled = false
  policy = jsonencode(
    {
      Statement = [
        {
          Action = "s3:GetBucketAcl"
          Effect = "Allow"
          Principal = {
            Service = "logs.us-east-1.amazonaws.com"
          }
          Resource = "arn:aws:s3:::bfd-insights-bfd-app-logs"
        },
        {
          Action = "s3:PutObject"
          Condition = {
            StringEquals = {
              "s3:x-amz-acl" = "bucket-owner-full-control"
            }
          }
          Effect = "Allow"
          Principal = {
            Service = "logs.us-east-1.amazonaws.com"
          }
          Resource = "arn:aws:s3:::bfd-insights-bfd-app-logs/*"
        },
        {
          Action = "s3:*"
          Condition = {
            Bool = {
              "aws:SecureTransport" = "false"
            }
          }
          Effect    = "Deny"
          Principal = "*"
          Resource = [
            "arn:aws:s3:::bfd-insights-bfd-app-logs",
            "arn:aws:s3:::bfd-insights-bfd-app-logs/*",
          ]
          Sid = "AllowSSLRequestsOnly"
        },
      ]
      Version = "2012-10-17"
    }
  )
  request_payer = "BucketOwner"
  tags          = {}
  tags_all      = {}

  grant {
    id = "c393fda6bb2079d44a0284a5164e871a4555039d6c6f973c9e5db5f4d7d76b1a"
    permissions = [
      "FULL_CONTROL",
    ]
    type = "CanonicalUser"
  }

  server_side_encryption_configuration {
    rule {
      bucket_key_enabled = false

      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }

  versioning {
    enabled    = false
    mfa_delete = false
  }
}
