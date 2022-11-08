# Get the users from ssm store and save them to a list for later
locals {
  synthea_developers = sort([for user in values(data.aws_iam_user.synthea) : user.user_name])
}

data "aws_ssm_parameter" "synthea" {
  name = "/bfd/global/terraform/sensitive/synthea_developers"
}

data "aws_iam_user" "synthea" {
  for_each  = toset(split(" ", nonsensitive(data.aws_ssm_parameter.synthea.value)))
  user_name = each.value
}

## Set up the bucket and its configuration
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

# Set up the synthea group, permissions, and users
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

resource "aws_iam_group_membership" "synthea" {
  name = "bfd-${local.env}-synthea"
  group = aws_iam_group.synthea.name
  users = local.synthea_developers
}
