locals {
  obects_to_replicate = [
      "Backup",
      "Done",
      "Hold",
      "Incoming",
      "RDA-Synthetic",
      "Sample",
      "Synthetic",
    ]
}

# alt region bucket
resource "aws_s3_bucket" "this_alt" {
  bucket = "${aws_s3_bucket.this.name}-${aws.alt.region}"
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
  name               = "bfd-${local.env}-${local.legacy_service}-crr-role"
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
  name        = "${aws_s3_bucket.this.name}-${aws.alt.region}-crr-policy"
  description = "Allows cross-region replication from ${aws_s3_bucket.this.name} to ${aws_s3_bucket.this.name}-${aws.alt.region}"
  policy      = data.aws_iam_policy_document.ccw_crr.json
}

data "aws_iam_policy_document" "crr" {
  # allow crr role to use kms keys
  statement {
    sid = "AllowKeyUsage"
    effect = "Allow"
    actions = [
      "kms:Decrypt",
      "kms:Encrypt",
      "kms:GenerateDataKey",
      "kms:ReEncrypt*",
      "kms:DescribeKey",
    ]
    resources = [
      "arn:aws:kms:${aws.region}:${data.aws_caller_identity.current.account_id}:key/${aws_kms_key.this.key_id}",
      "arn:aws:kms:${aws.alt.region}:${data.aws_caller_identity.current.account_id}:key/${data.aws_kms_key.this.key_id}",
    ]
  }
  statement {
    sid = "AllowReadBucketConfig"
    effect = "Allow"
    actions = [
      "s3:GetReplicationConfiguration",
      "s3:ListBucket",
    ]
    resources = [aws_s3_bucket.this.arn]
  }

  statement {
    sid = "AllowGetVersionConfig"
    effect = "Allow"
    actions = [
      "s3:GetObjectVersionForReplication",
      "s3:GetObjectVersionAcl",
      "s3:GetObjectVersionTagging",
    ]
    resources = flatten([
      for object in local.obects_to_replicate : [
        "${aws_s3_bucket.this.arn}/${object}",
        "${aws_s3_bucket.this.arn}/${object}/*",
      ]
    ])
  }

  statement {
    sid = "AllowReplicateObjects"
    effect = "Allow"
    actions = [
      "s3:ReplicateObject",
      "s3:ReplicateDelete",
      "s3:ReplicateTags",
    ]
    resources = flatten([
      for object in local.obects_to_replicate : [
        "${aws_s3_bucket.destination.arn}/${object}",
        "${aws_s3_bucket.destination.arn}/${object}/*",
      ]
    ])
  }
}
