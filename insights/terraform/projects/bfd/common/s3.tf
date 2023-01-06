resource "aws_s3_bucket" "bfd-insights-bfd-app-logs" {
  bucket              = "bfd-insights-bfd-app-logs"
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

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.bfd-insights-bfd-app-logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# TODO: Replace the following when/if insights Terraform is merged with main Terraform
resource "aws_s3_bucket_notification" "bucket_notifications" {
  bucket = module.bucket.id

  dynamic "lambda_function" {
    for_each = data.aws_lambda_function.server_regression_glue_triggers

    content {
      events = [
        "s3:ObjectCreated:*",
      ]
      filter_prefix       = "databases/bfd-insights-bfd-${lambda_function.key}/bfd_insights_bfd_${replace(lambda_function.key, "-", "_")}_server_regression/"
      filter_suffix       = ".stats.json"
      id                  = "bfd-${lambda_function.key}-server-regression-glue-trigger"
      lambda_function_arn = lambda_function.value.arn
    }
  }
  dynamic "lambda_function" {
    for_each = data.aws_lambda_function.bfd_insights_error_slack

    content {
      events = [
        "s3:ObjectCreated:*",
      ]
      filter_prefix       = "databases/bfd-insights-bfd-${lambda_function.key}/bfd_insights_bfd_${replace(lambda_function.key, "-", "_")}_api_requests_errors/"
      id                  = "bfd-${lambda_function.key}-bfd-insights-error-slack"
      lambda_function_arn = lambda_function.value.arn
    }
  }
}