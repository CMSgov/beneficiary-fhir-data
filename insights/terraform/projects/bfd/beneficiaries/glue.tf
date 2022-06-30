# Beneficiaries
#
# Beneficiaries, which captures when a beneficiary is queried

# Glue Catalog Table to hold Beneficiaries
module "beneficiaries-table" {
  source      = "../../../modules/table"
  table       = "${local.full_name_underscore}_api_requests_beneficiaries"
  description = "One row per beneficiary query, with the date of the request"
  database    = local.database
  bucket      = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk  = data.aws_kms_key.kms_key.arn
  tags        = local.tags
  partitions  = [
    {
      name    = "year"
      type    = "string"
      comment = "Year of request"
    },
    {
      name    = "month"
      type    = "string"
      comment = "Month of request"
    },
    {
      name    = "day"
      type    = "string"
      comment = "Day of request"
    }
  ]
  columns     = []
}

# S3 Object for Glue Script
resource "aws_s3_object" "bfd-populate-beneficiaries" {
  bucket             = data.aws_s3_bucket.bfd-insights-bucket.id
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/${local.environment}/bfd_populate_api_requests_beneficiaries.py"
  metadata           = {}
  storage_class      = "STANDARD"
  source             = "glue_src/bfd_populate_api_requests_beneficiaries.py"
}

# Glue Job to populate the beneficiaries table
resource "aws_glue_job" "bfd-populate-beneficiaries-job" {
  name                      = "${local.full_name}-populate-beneficiaries"
  description               = "Populate the Beneficiaries table"
  glue_version              = "3.0"
  max_retries               = 0
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = data.aws_iam_role.glue-role.arn
  timeout                   = 2880
  worker_type               = "G.1X"
  connections               = []

  default_arguments = {
    "--TempDir"                          = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/temporary/${local.environment}/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--job-bookmark-option"              = "job-bookmark-enable"
    "--job-language"                     = "python"
    "--sourceDatabase"                   = local.database
    "--sourceTable"                      = "${local.full_name_underscore}_api_requests"
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = local.database
    "--targetTable"                      = module.beneficiaries-table.name
  }

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/${aws_s3_object.bfd-populate-beneficiaries.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}

# Crawler for the Beneficiaries table
resource "aws_glue_crawler" "beneficiaries-crawler" {
  classifiers   = []
  database_name = local.database
  configuration = jsonencode(
    {
      CrawlerOutput = {
        Partitions = {
          AddOrUpdateBehavior = "InheritFromTable"
        }
      }
      Grouping = {
        TableGroupingPolicy = "CombineCompatibleSchemas"
      }
      Version = 1
    }
  )
  name     = "${local.full_name}-beneficiaries-crawler"
  role     = data.aws_iam_role.glue-role.arn

  catalog_target {
    database_name = local.database
    tables = [
      module.beneficiaries-table.name,
    ]
  }

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}


# Beneficiary Unique
#
# Beneficiary Unique table to track the first time each beneficiary was queried.

# Glue Table for unique beneficiaries
module "beneficiaries-unique-table" {
  source      = "../../../modules/table"
  table       = "${local.full_name_underscore}_api_requests_beneficiaries_unique"
  description = "One row per beneficiary and the date first seen"
  database    = local.database
  bucket      = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk  = data.aws_kms_key.kms_key.arn
  tags        = local.tags

  partitions  = [
    {
      name    = "year"
      type    = "string"
      comment = "Year of request"
    },
    {
      name    = "month"
      type    = "string"
      comment = "Month of request"
    },
  ]

  columns     = [
    {
      name    = "bene_id"
      type    = "bigint"
      comment = "Beneficiary ID"
    },
    {
      name    = "first_seen"
      type    = "timestamp"
      comment = "Date first seen"
    }
  ]
}

# S3 Object for the Glue Script
resource "aws_s3_object" "bfd-populate-beneficiary-unique" {
  bucket             = data.aws_s3_bucket.bfd-insights-bucket.id
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/${local.environment}/bfd_populate_api_requests_beneficiary_unique.py"
  metadata           = {}
  storage_class      = "STANDARD"
  source             = "glue_src/bfd_populate_api_requests_beneficiary_unique.py"
}

# Glue Job to populate the beneficiary_unique table
resource "aws_glue_job" "bfd-populate-beneficiary-unique-job" {
  connections = []
  default_arguments = {
    "--TempDir"                          = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/temporary/${local.environment}/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--initialize"                       = "True"
    "--job-bookmark-option"              = "job-bookmark-disable" # We need to process all records
    "--job-language"                     = "python"
    "--sourceDatabase"                   = local.database
    "--sourceTable"                      = module.beneficiaries-table.name
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = local.database
    "--targetTable"                      = module.beneficiaries-unique-table.name
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "${local.full_name}-populate-beneficiary-unique"
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = data.aws_iam_role.glue-role.arn
  timeout                   = 2880
  worker_type               = "G.1X"

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/${aws_s3_object.bfd-populate-beneficiary-unique.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}

# Crawler for the Unique Beneficiaries table
resource "aws_glue_crawler" "beneficiaries-unique-crawler" {
  classifiers   = []
  database_name = local.database
  configuration = jsonencode(
    {
      CrawlerOutput = {
        Partitions = {
          AddOrUpdateBehavior = "InheritFromTable"
        }
      }
      Grouping = {
        TableGroupingPolicy = "CombineCompatibleSchemas"
      }
      Version = 1
    }
  )
  name     = "${local.full_name}-beneficiaries-unique-crawler"
  role     = data.aws_iam_role.glue-role.arn

  catalog_target {
    database_name = local.database
    tables = [
      module.beneficiaries-unique-table.name
    ]
  }

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}


# Glue Workflow
#
# Organizes the Glue jobs / crawlers and runs them in sequence

# Trigger for Populate Beneficiaries Job
resource "aws_glue_trigger" "bfd-populate-beneficiaries-job-trigger" {
  name          = "${local.full_name}-populate-beneficiaries-job-trigger"
  description   = "Trigger to start the Populate Beneficiaries Glue Job whenever the Crawler completes successfully"
  workflow_name = local.glue_workflow_name
  type          = "CONDITIONAL"

  actions {
    job_name = aws_glue_job.bfd-populate-beneficiaries-job.name
  }

  predicate {
    conditions {
      crawler_name = "${local.full_name}-api-requests-crawler" # From api-requests
      crawl_state  = "SUCCEEDED"
    }
  }
}

# Trigger for Populate Beneficiaries Crawler
resource "aws_glue_trigger" "bfd-beneficiaries-crawler-trigger" {
  name          = "${local.full_name}-beneficiaries-crawler-trigger"
  description   = "Trigger to start the Beneficiaries Crawler whenever the Populate Beneficiaries Job completes successfully"
  workflow_name = local.glue_workflow_name
  type          = "CONDITIONAL"

  actions {
    crawler_name = aws_glue_crawler.beneficiaries-crawler.name
  }

  predicate {
    conditions {
      job_name = aws_glue_job.bfd-populate-beneficiaries-job.name
      state  = "SUCCEEDED"
    }
  }
}

# Trigger for Populate Beneficiaries Unique Job
resource "aws_glue_trigger" "bfd-populate-beneficiaries-unique-job-trigger" {
  name          = "${local.full_name}-populate-beneficiaries-unique-job-trigger"
  description   = "Trigger to start the Populate Beneficiaries Unique Job whenever the Beneficiaries Crawler completes successfully"
  workflow_name = local.glue_workflow_name
  type          = "CONDITIONAL"

  actions {
    job_name = aws_glue_job.bfd-populate-beneficiary-unique-job.name
  }

  predicate {
    conditions {
      crawler_name = aws_glue_crawler.beneficiaries-crawler.name
      crawl_state  = "SUCCEEDED"
    }
  }
}

# Trigger for Populate Beneficiaries Unique Crawler
resource "aws_glue_trigger" "bfd-beneficiaries-unique-crawler-trigger" {
  name          = "${local.full_name}-beneficiaries-unique-crawler-trigger"
  description   = "Trigger to start the Beneficiaries Unique Crawler whenever the Populate Beneficiaries Unique Job completes successfully"
  workflow_name = local.glue_workflow_name
  type          = "CONDITIONAL"

  actions {
    crawler_name = aws_glue_crawler.beneficiaries-unique-crawler.name
  }

  predicate {
    conditions {
      job_name = aws_glue_job.bfd-populate-beneficiary-unique-job.name
      state  = "SUCCEEDED"
    }
  }
}
