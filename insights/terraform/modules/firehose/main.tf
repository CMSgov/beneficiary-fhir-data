data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "moderate_bucket" {
  bucket      = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

locals {
  full_name   = "bfd-insights-${var.project}-${var.stream}"
  account_id  = data.aws_caller_identity.current.account_id
  bucket_arn  = data.aws_s3_bucket.moderate_bucket.arn
}

resource "aws_iam_role" "firehose" {
  name = local.full_name
  tags = var.tags

  assume_role_policy = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Principal": {
          "Service": "firehose.amazonaws.com"
        },
        "Effect": "Allow",
        "Sid": ""
      }
    ]
  }
  EOF
}

resource "aws_iam_role_policy_attachment" "main" {
  role       = aws_iam_role.firehose.name
  policy_arn = aws_iam_policy.firehose.arn
}

resource "aws_kinesis_firehose_delivery_stream" "main" {
  name                  = local.full_name
  tags                  = var.tags
  destination           = "extended_s3"

  server_side_encryption {
    enabled = true
  } 

  extended_s3_configuration {
    role_arn            = aws_iam_role.firehose.arn
    bucket_arn          = local.bucket_arn
    buffer_size         = var.buffer_size
    buffer_interval     = var.buffer_interval
    compression_format  = "Snappy"
    prefix              = "projects/${var.project}/${var.stream}_firehose/dt=!{timestamp:yyyy-MM-dd-HH}/"
    error_output_prefix = "projects/${var.project}/${var.stream}_firehose_errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"
  }
}