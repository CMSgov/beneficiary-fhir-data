resource "aws_s3_bucket" "synthea" {
  bucket = "bfd-mgmt-synthea"
}

resource "aws_s3_bucket_public_access_block" "synthea" {
  bucket = aws_s3_bucket.synthea.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "synthea" {
  bucket = aws_s3_bucket.synthea.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_iam_group" "synthea" {
  name = "bfd-${local.env}-synthea"
  path = "/"
}

resource "aws_iam_policy_attachment" "synthea" {
  name       = "bfd-${local.env}-synthea"
  groups     = [aws_iam_group.synthea.name]
  policy_arn = aws_iam_policy.synthea.arn
}

resource "aws_iam_policy" "synthea" {
  name = "bfd-${local.env}-synthea-rw-s3"
  policy = jsonencode({
    "Statement" : [
      {
        "Action" : [
          "s3:ListBucket",
          "s3:GetBucketLocation"
        ],
        "Effect" : "Allow",
        "Resource" : [
          aws_s3_bucket.synthea.arn,
        ]
      },
      {
        "Action" : [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject"
        ],
        "Effect" : "Allow",
        "Resource" : [
          "${aws_s3_bucket.synthea.arn}/*",
        ]
      }
    ],
    "Version" : "2012-10-17"
  })
}
