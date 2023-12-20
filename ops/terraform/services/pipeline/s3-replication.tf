locals {
  obects_to_replicate = [
    "Backup",
    "Done",
    "Hold",
    # "Incoming",
    "RDA-Synthetic",
    "Sample",
    "Synthetic",
  ]
}

# alt region bucket
resource "aws_s3_bucket" "this_alt" {
  provider = aws.alt

  bucket = "${aws_s3_bucket.this.id}-${local.alt_region}"

  tags = {
    Layer   = local.layer,
    role    = local.legacy_service
    Note    = "CCW backup bucket"
    Purpose = "ETL PUT"
    UsedBy  = "CCW"
  }
}

# crr assume role
resource "aws_iam_role" "crr" {
  name = "bfd-${local.env}-${local.legacy_service}-crr-role"
  assume_role_policy = jsonencode({
    version = "2012-10-17"
    statement = [
      {
        effect = "Allow"
        principal = {
          service = "s3.amazonaws.com"
        }
        action = "sts:AssumeRole"
      }
    ]
  })

}

# crr policy
resource "aws_iam_policy" "crr" {
  name        = "${aws_s3_bucket.this.id}-${local.alt_region}-crr-policy"
  description = "Allows cross-region replication from ${aws_s3_bucket.this.id} to ${aws_s3_bucket.this.id}-${local.alt_region}"
  policy      = data.aws_iam_policy_document.ccw_crr.json
}

data "aws_iam_policy_document" "crr" {
  # allow crr role to use kms keys
  statement {
    sid    = "AllowKeyUsage"
    effect = "Allow"
    actions = [
      "kms:Decrypt",
      "kms:Encrypt",
      "kms:GenerateDataKey",
      "kms:ReEncrypt*",
      "kms:DescribeKey",
    ]
    resources = [
      data.aws_kms_key.cmk.arn,
      data.aws_kms_key.cmk_alt.arn,
    ]
  }

  statement {
    sid    = "AllowReadBucketConfig"
    effect = "Allow"
    actions = [
      "s3:GetReplicationConfiguration",
      "s3:ListBucket",
    ]
    resources = [aws_s3_bucket.this.arn]
  }

  statement {
    sid    = "AllowGetVersionConfig"
    effect = "Allow"
    actions = [
      "s3:GetObjectVersionForReplication",
      "s3:GetObjectVersionAcl",
      "s3:GetObjectVersionTagging",
    ]
    resources = flatten([
      for folder in local.obects_to_replicate : [
        "${aws_s3_bucket.this.arn}/${folder}",
        "${aws_s3_bucket.this.arn}/${folder}/*",
      ]
    ])
  }

  statement {
    sid    = "AllowReplicateObjects"
    effect = "Allow"
    actions = [
      "s3:ReplicateObject",
      "s3:ReplicateDelete",
      "s3:ReplicateTags",
    ]
    resources = flatten([
      for folder in local.obects_to_replicate : [
        "${aws_s3_bucket.destination.arn}/${folder}",
        "${aws_s3_bucket.destination.arn}/${folder}/*",
      ]
    ])
  }
}
