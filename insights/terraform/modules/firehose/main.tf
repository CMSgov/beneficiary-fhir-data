data "aws_caller_identity" "current" {}

locals {
  full_name   = "bfd-insights-${var.sensitivity}-${var.stream}"
  account_id  = data.aws_caller_identity.current.account_id
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
    bucket_arn          = var.bucket_arn
    buffer_size         = 1
    buffer_interval     = 60
    compression_format  = "Snappy"
    prefix              = "firehose/${var.stream}/dt=!{timestamp:yyyy-MM-dd-HH}/"
    error_output_prefix = "firehose/${var.stream}-errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"
  }
}