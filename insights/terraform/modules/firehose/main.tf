data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "main" {
  bucket      = var.bucket
}

data "aws_kms_key" "bucket_cmk" {
  key_id      = var.bucket_cmk
}

locals {
  full_name   = "bfd-insights-${var.database}-${var.stream}"
  account_id  = data.aws_caller_identity.current.account_id
  bucket_arn  = data.aws_s3_bucket.main.arn
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

  # Encrypt while processing
  server_side_encryption {
    enabled = true
  } 

  extended_s3_configuration {
    role_arn            = aws_iam_role.firehose.arn
    bucket_arn          = local.bucket_arn
    kms_key_arn         = data.aws_kms_key.bucket_cmk.arn # Encrypt on delivery
    buffer_size         = var.buffer_size
    buffer_interval     = var.buffer_interval
    compression_format  = "GZIP"
    prefix              = "databases/${var.database}/${var.stream}/dt=!{timestamp:yyyy-MM-dd}/"
    error_output_prefix = "databases/${var.database}/${var.stream}_errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"
  }
}