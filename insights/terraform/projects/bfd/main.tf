locals {
  tags       = { business = "OEDA", application = "bfd-insights", project = "bfd" }
  database   = "bfd"
  project    = "bfd"
  table      = "api-requests"
  full_name  = "bfd-insights-${local.database}-${local.table}"
  account_id = data.aws_caller_identity.current.account_id
}

module "bucket" {
  source      = "../../modules/bucket"
  name        = local.database
  sensitivity = "high"
  tags        = local.tags
  full_groups = [] # prevent bucket module from attempting to attach policy
}

module "database" {
  source     = "../../modules/database"
  database   = local.database
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  tags       = local.tags
}

resource "aws_glue_catalog_table" "api_requests" {
  catalog_id    = "577373831711"
  database_name = "bfd"
  name          = "api_requests"
  owner         = "owner"
  parameters = {
    "CrawlerSchemaDeserializerVersion" = "1.0"
    "CrawlerSchemaSerializerVersion"   = "1.0"
    "UPDATED_BY_CRAWLER"               = "bfd"
    "averageRecordSize"                = "6006"
    "classification"                   = "json"
    "compressionType"                  = "gzip"
    "objectCount"                      = "2"
    "recordCount"                      = "2"
    "sizeKey"                          = "2719"
    "typeOfData"                       = "file"
  }
  retention  = 0
  table_type = "EXTERNAL_TABLE"

  partition_keys {
    name = "year"
    type = "string"
  }
  partition_keys {
    name = "month"
    type = "string"
  }
  partition_keys {
    name = "day"
    type = "string"
  }

  storage_descriptor {
    bucket_columns    = []
    compressed        = true
    input_format      = "org.apache.hadoop.mapred.TextInputFormat"
    location          = "s3://bfd-insights-bfd-577373831711/databases/bfd/api_requests/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
    parameters = {
      "CrawlerSchemaDeserializerVersion" = "1.0"
      "CrawlerSchemaSerializerVersion"   = "1.0"
      "UPDATED_BY_CRAWLER"               = "bfd"
      "averageRecordSize"                = "6006"
      "classification"                   = "json"
      "compressionType"                  = "gzip"
      "objectCount"                      = "2"
      "recordCount"                      = "2"
      "sizeKey"                          = "2719"
      "typeOfData"                       = "file"
    }
    stored_as_sub_directories = false

    columns {
      name       = "timestamp"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "level"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "thread"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc"
      parameters = {}
      type       = "struct<database_query.eobs_by_bene_id.hha.type:string,jpa_query.eobs_by_bene_id.snf.duration_milliseconds:string,database_query.eobs_by_bene_id.hha.batch_size:string,jpa_query.eobs_by_bene_id.carrier.record_count:string,http_access.response.header.Date:string,database_query.eobs_by_bene_id.snf.success:string,database_query.eobs_by_bene_id.outpatient.type:string,jpa_query.eobs_by_bene_id.inpatient.duration_nanoseconds:string,jpa_query.eobs_by_bene_id.inpatient.duration_milliseconds:string,jpa_query.eobs_by_bene_id.inpatient.record_count:string,jpa_query.eobs_by_bene_id.outpatient.record_count:string,database_query.eobs_by_bene_id.inpatient.type:string,http_access.response.status:string,jpa_query.eobs_by_bene_id.dme.record_count:string,jpa_query.eobs_by_bene_id.pde.duration_milliseconds:string,jpa_query.eobs_by_bene_id.carrier.duration_milliseconds:string,jpa_query.eobs_by_bene_id.snf.duration_nanoseconds:string,jpa_query.eobs_by_bene_id.carrier.duration_nanoseconds:string,http_access.request.http_method:string,database_query.eobs_by_bene_id.pde.size:string,database_query.eobs_by_bene_id.carrier.size:string,http_access.request.query_string:string,jpa_query.eobs_by_bene_id.outpatient.duration_milliseconds:string,database_query.eobs_by_bene_id.hospice.datasource_name:string,jpa_query.eobs_by_bene_id.dme.duration_milliseconds:string,http_access.response.duration_milliseconds:string,jpa_query.eobs_by_bene_id.dme.duration_nanoseconds:string,database_query.eobs_by_bene_id.hha.batch:string,database_query.eobs_by_bene_id.outpatient.batch_size:string,database_query.eobs_by_bene_id.hha.size:string,database_query.eobs_by_bene_id.pde.batch_size:string,database_query.eobs_by_bene_id.hha.success:string,database_query.eobs_by_bene_id.inpatient.size:string,database_query.eobs_by_bene_id.inpatient.success:string,database_query.eobs_by_bene_id.snf.batch_size:string,database_query.eobs_by_bene_id.dme.size:string,database_query.eobs_by_bene_id.snf.size:string,database_query.eobs_by_bene_id.hospice.size:string,http_access.request.header.User-Agent:string,database_query.eobs_by_bene_id.pde.batch:string,jpa_query.eobs_by_bene_id.snf.record_count:string,jpa_query.eobs_by_bene_id.pde.record_count:string,database_query.eobs_by_bene_id.hha.datasource_name:string,database_query.eobs_by_bene_id.pde.type:string,jpa_query.eobs_by_bene_id.hha.record_count:string,jpa_query.eobs_by_bene_id.hospice.duration_milliseconds:string,database_query.eobs_by_bene_id.hha.duration_milliseconds:string,database_query.eobs_by_bene_id.carrier.success:string,http_access.request.clientSSL.DN:string,jpa_query.eobs_by_bene_id.hha.duration_nanoseconds:string,database_query.eobs_by_bene_id.snf.batch:string,database_query.eobs_by_bene_id.outpatient.datasource_name:string,http_access.request.header.Accept:string,database_query.eobs_by_bene_id.dme.success:string,database_query.eobs_by_bene_id.hospice.success:string,database_query.eobs_by_bene_id.carrier.duration_milliseconds:string,jpa_query.eobs_by_bene_id.hha.duration_milliseconds:string,http_access.request.operation:string,database_query.eobs_by_bene_id.dme.duration_milliseconds:string,database_query.eobs_by_bene_id.pde.duration_milliseconds:string,database_query.eobs_by_bene_id.snf.datasource_name:string,database_query.eobs_by_bene_id.inpatient.datasource_name:string,http_access.response.header.X-Request-ID:string,database_query.eobs_by_bene_id.snf.type:string,jpa_query.eobs_by_bene_id.pde.duration_nanoseconds:string,database_query.eobs_by_bene_id.hospice.type:string,database_query.eobs_by_bene_id.inpatient.batch_size:string,database_query.eobs_by_bene_id.dme.type:string,database_query.eobs_by_bene_id.snf.duration_milliseconds:string,http_access.response.header.X-Powered-By:string,database_query.eobs_by_bene_id.carrier.batch:string,database_query.eobs_by_bene_id.outpatient.success:string,database_query.eobs_by_bene_id.inpatient.duration_milliseconds:string,http_access.request_type:string,database_query.eobs_by_bene_id.inpatient.batch:string,database_query.eobs_by_bene_id.outpatient.batch:string,database_query.eobs_by_bene_id.pde.success:string,database_query.eobs_by_bene_id.pde.datasource_name:string,database_query.eobs_by_bene_id.outpatient.size:string,database_query.eobs_by_bene_id.dme.datasource_name:string,http_access.request.header.Host:string,jpa_query.eobs_by_bene_id.hospice.duration_nanoseconds:string,jpa_query.eobs_by_bene_id.hospice.record_count:string,database_query.eobs_by_bene_id.carrier.batch_size:string,jpa_query.eobs_by_bene_id.outpatient.duration_nanoseconds:string,http_access.response.header.Content-Type:string,database_query.eobs_by_bene_id.hospice.duration_milliseconds:string,database_query.eobs_by_bene_id.outpatient.duration_milliseconds:string,database_query.eobs_by_bene_id.hospice.batch:string,http_access.response.header.Last-Modified:string,database_query.eobs_by_bene_id.carrier.type:string,database_query.eobs_by_bene_id.dme.batch:string,http_access.request.url:string,database_query.eobs_by_bene_id.hospice.batch_size:string,http_access.request.uri:string,database_query.eobs_by_bene_id.carrier.datasource_name:string,bene_id:string,database_query.eobs_by_bene_id.dme.batch_size:string>"
    }
    columns {
      name       = "logger"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "message"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "context"
      parameters = {}
      type       = "string"
    }

    ser_de_info {
      parameters = {
        "paths" = "context,level,logger,mdc,message,thread,timestamp"
      }
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }
  }
}

module "workgroup" {
  source     = "../../modules/workgroup"
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  name       = local.database
  tags       = local.tags
}

module "glue_jobs" {
  source  = "../../modules/jobs"
  project = local.project
  tags    = local.tags

  buckets = [
    { bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn }
  ]
}

resource "aws_glue_crawler" "glue_crawler" {
  name          = local.project
  database_name = local.database
  role          = "bfd-insights/bfd-insights-${local.project}-glue-role"
  s3_target {
    path = "s3://${module.bucket.id}/databases/${local.project}"
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

data "aws_caller_identity" "current" {}

data "aws_iam_group" "bfd_analysts" {
  group_name = "bfd-insights-analysts"
}

resource "aws_iam_group_policy" "poc" {
  name   = "foo"
  group  = data.aws_iam_group.bfd_analysts.group_name
  policy = module.bucket.iam_full_policy_body
}

resource "aws_iam_policy" "firehose" {
  description = "Allow firehose delivery to bfd-insights-bfd-577373831711"
  name        = "bfd-insights-bfd-api-requests"
  path        = "/bfd-insights/"
  policy = jsonencode(
    {
      Statement = [
        {
          Action = [
            "glue:GetTable",
            "glue:GetTableVersion",
            "glue:GetTableVersions",
          ]
          Effect   = "Allow"
          Resource = "*"
          Sid      = ""
        },
        {
          Action = [
            "s3:AbortMultipartUpload",
            "s3:GetBucketLocation",
            "s3:GetObject",
            "s3:ListBucket",
            "s3:ListBucketMultipartUploads",
            "s3:PutObject",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:s3:::bfd-insights-bfd-577373831711",
            "arn:aws:s3:::bfd-insights-bfd-577373831711/*",
          ]
          Sid = ""
        },
        {
          Action = [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*",
            "kms:DescribeKey",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea",
          ]
        },
        {
          Action = [
            "logs:PutLogEvents",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:logs:us-east-1:577373831711:log-group:/aws/kinesisfirehose/bfd-insights-bfd-api-requests:log-stream:*",
          ]
          Sid = ""
        },
        {
          Action = [
            "kinesis:DescribeStream",
            "kinesis:GetShardIterator",
            "kinesis:GetRecords",
            "kinesis:ListShards",
          ]
          Effect   = "Allow"
          Resource = "arn:aws:kinesis:us-east-1:577373831711:stream/bfd-insights-bfd-api-requests"
          Sid      = ""
        },
      ]
      Version = "2012-10-17"
    }
  )
  tags     = {}
  tags_all = {}
}

resource "aws_iam_role" "firehose" {
  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "firehose.amazonaws.com"
          }
          Sid = ""
        },
      ]
      Version = "2012-10-17"
    }
  )
  force_detach_policies = false
  managed_policy_arns = [
    "arn:aws:iam::577373831711:policy/bfd-insights/bfd-insights-bfd-api-requests",
  ]
  max_session_duration = 3600
  name                 = "bfd-insights-bfd-api-requests"
  path                 = "/"
  tags = {
    "application" = "bfd-insights"
    "business"    = "OEDA"
    "project"     = "bfd"
  }
  tags_all = {
    "application" = "bfd-insights"
    "business"    = "OEDA"
    "project"     = "bfd"
  }

  inline_policy {
    name = "bfd-transform-lambda"
    policy = jsonencode(
      {
        Statement = [
          {
            Action   = "lambda:InvokeFunction"
            Effect   = "Allow"
            Resource = "arn:aws:lambda:us-east-1:577373831711:function:bfd-transform:$LATEST"
            Sid      = "VisualEditor0"
          },
        ]
        Version = "2012-10-17"
      }
    )
  }
}

resource "aws_iam_role_policy_attachment" "main" {
  policy_arn = "arn:aws:iam::577373831711:policy/bfd-insights/bfd-insights-bfd-api-requests"
  role       = "bfd-insights-bfd-api-requests"
}

resource "aws_kinesis_firehose_delivery_stream" "main" {
  arn            = "arn:aws:firehose:us-east-1:577373831711:deliverystream/bfd-insights-bfd-api-requests"
  destination    = "extended_s3"
  destination_id = "destinationId-000000000001"
  name           = "bfd-insights-bfd-api-requests"
  tags = {
    "application" = "bfd-insights"
    "business"    = "OEDA"
    "project"     = "bfd"
  }
  tags_all = {
    "application" = "bfd-insights"
    "business"    = "OEDA"
    "project"     = "bfd"
  }
  version_id = "3"

  extended_s3_configuration {
    bucket_arn          = "arn:aws:s3:::bfd-insights-bfd-577373831711"
    buffer_interval     = 60
    buffer_size         = 5
    compression_format  = "GZIP"
    error_output_prefix = "databases/bfd/api_requests_errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"
    kms_key_arn         = "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea"
    prefix              = "databases/bfd/api_requests/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    role_arn            = "arn:aws:iam::577373831711:role/bfd-insights-bfd-api-requests"
    s3_backup_mode      = "Disabled"

    cloudwatch_logging_options {
      enabled = false
    }

    processing_configuration {
      enabled = true

      processors {
        type = "Lambda"

        parameters {
          parameter_name  = "LambdaArn"
          parameter_value = "arn:aws:lambda:us-east-1:577373831711:function:bfd-transform:$LATEST"
        }
      }
    }
  }

  server_side_encryption {
    enabled  = true
    key_type = "AWS_OWNED_CMK"
  }
}

resource "aws_cloudwatch_log_subscription_filter" "bfd-test-access-log-subscription" {
  name            = "bfd-test-access-log-subscription"
  log_group_name  = "/bfd/test/bfd-server/access.json"
  filter_pattern  = ""
  destination_arn = "arn:aws:firehose:us-east-1:${local.account_id}:deliverystream/${local.full_name}"
  role_arn        = aws_iam_role.cloudwatch_role.arn
}

data "aws_iam_policy_document" "trust_rel_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["logs.us-east-1.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cloudwatch_role" {
  name               = "${local.full_name}-cwl2firehose-role"
  assume_role_policy = data.aws_iam_policy_document.trust_rel_assume_role_policy.json

  inline_policy {
    name = "${local.full_name}-cwl2firehose-policy"

    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Action   = ["firehose:*"]
          Effect   = "Allow"
          Resource = ["arn:aws:firehose:us-east-1:${local.account_id}:*"]
        },
      ]
    })
  }
}

resource "aws_lambda_function" "bfd-transform" {
  architectures = [
    "x86_64",
  ]
  description                    = "An Amazon Kinesis Firehose stream processor that extracts individual log events from records sent by Cloudwatch Logs subscription filters."
  function_name                  = "bfd-transform"
  handler                        = "index.handler"
  layers                         = []
  memory_size                    = 128
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = "arn:aws:iam::577373831711:role/service-role/bfd-transform-role-rlenc44a"
  runtime                        = "nodejs14.x"
  source_code_hash               = "xfOr6tQXzD/YDKTg0NT9+Dx+J26cyPLuDgCqWB+VeDU="
  tags = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor"
  }
  tags_all = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor"
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

resource "aws_glue_job" "bfd-insights-bfd-unique-bene-combiner" {
  connections = []
  default_arguments = {
    "--TempDir"                          = "s3://aws-glue-assets-577373831711-us-east-1/temporary/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--initialize"                       = "True"
    "--job-bookmark-option"              = "job-bookmark-disable"
    "--job-language"                     = "python"
    "--sourceTable"                      = "api_requests"
    "--targetTable"                      = "beneficiaries_unique"
    "--spark-event-logs-path"            = "s3://aws-glue-assets-577373831711-us-east-1/sparkHistoryLogs/"
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "bfd-insights-bfd-unique-bene-combiner"
  non_overridable_arguments = {}
  number_of_workers         = 2
  role_arn                  = "arn:aws:iam::577373831711:role/bfd-insights/bfd-insights-bfd-glue-role"
  tags                      = {}
  tags_all                  = {}
  timeout                   = 2880
  worker_type               = "G.1X"

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://aws-glue-assets-577373831711-us-east-1/scripts/bfd-insights-bfd-unique-bene-combiner.py"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}
