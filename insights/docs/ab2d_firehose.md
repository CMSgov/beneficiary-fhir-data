# AB2D Firehose Setup

# Bucket

The bucket for AB2D data is `bfd-insights-ab2d-BFDACCTNUM`

The CMK for the bucket is `arn:aws:kms:us-east-1:BFDACCTNUM:key/KEYID`

# Kinesis Firehose Setup

Here's the terraform template for setting up a firehose. 

```
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
```

Key aspects include:
- Partition by delivery time date
- Use a stream name that is the table name
- Put all tables under the `ab2d` database
- Use the "GZIP" compression format. Saves $.
- Don't mix error files with the stream data 

## Policy 
Here's the template for the firehose IAM policy. The sections on getting S3 and KMS access is critical. 

```
resource "aws_iam_policy" "firehose" {
  name        = local.full_name
  path        = "/bfd-insights/"
  description = "Allow firehose delivery to ${var.bucket}"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "",
            "Effect": "Allow",
            "Action": [
                "glue:GetTable",
                "glue:GetTableVersion",
                "glue:GetTableVersions"
            ],
            "Resource": "*"
        },
        {
            "Sid": "",
            "Effect": "Allow",
            "Action": [
                "s3:AbortMultipartUpload",
                "s3:GetBucketLocation",
                "s3:GetObject",
                "s3:ListBucket",
                "s3:ListBucketMultipartUploads",
                "s3:PutObject"
            ],
            "Resource": [
                "${local.bucket_arn}",
                "${local.bucket_arn}/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "kms:Encrypt",
                "kms:Decrypt",
                "kms:ReEncrypt*",
                "kms:GenerateDataKey*",
                "kms:DescribeKey"
            ],
            "Resource": [
                "${data.aws_kms_key.bucket_cmk.arn}"
            ]
        },
        {
            "Sid": "",
            "Effect": "Allow",
            "Action": [
                "logs:PutLogEvents"
            ],
            "Resource": [
                "arn:aws:logs:us-east-1:${local.account_id}:log-group:/aws/kinesisfirehose/${local.full_name}:log-stream:*"
            ]
        },
        {
            "Sid": "",
            "Effect": "Allow",
            "Action": [
                "kinesis:DescribeStream",
                "kinesis:GetShardIterator",
                "kinesis:GetRecords",
                "kinesis:ListShards"
            ],
            "Resource": "arn:aws:kinesis:us-east-1:${local.account_id}:stream/${local.full_name}"
        }
    ]
}
EOF
}
```
