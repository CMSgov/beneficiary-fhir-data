# TODO:
# - lifecycle policy to prune delete marker objects after n days
locals {
  alt_bucket_storage_class = "STANDARD_IA"
  objects_to_replicate = [
    "Backup",
    "Done",
    "Hold",
    "Incoming",
    "RDA-Synthetic",
    "Sample",
    "Synthetic/Incoming",
    "Synthetic/Done",
    "Synthetic/Failed",
  ]
}

# S3 replication destination bucket. During a failover event, this bucket will need to be imported and treated as the
# primary bucket.
resource "aws_s3_bucket" "this_alt" {
  provider = aws.alt

  bucket = "${aws_s3_bucket.this.id}-${local.alt_region}"

  tags = {
    Layer   = local.layer,
    role    = local.legacy_service
    Note    = "Replication bucket for ${local.legacy_service} in ${local.alt_region}"
    Purpose = "ETL PUT"
    UsedBy  = "CCW"
  }
}

# During normal operations, we will send the alt bucket's logs to the primary logging bucket. This may need to be
# changed during a failover event if the primary bucket is not available.
resource "aws_s3_bucket_logging" "this_alt" {
  count = local.is_ephemeral_env || local.primary_region == local.alt_region ? 0 : 1

  bucket        = aws_s3_bucket.this_alt.id
  target_bucket = aws_s3_bucket.this.id
  target_prefix = replace("${local.legacy_service}_${local.alt_region}_s3_access_logs/", "-", "_")
}

# replication assume role
resource "aws_iam_role" "replication" {
  name = "bfd-${local.env}-${local.legacy_service}-replication-role"
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

# replication policy
resource "aws_iam_policy" "replication" {
  name        = "${aws_s3_bucket.this_alt.id}-replication-policy"
  description = "Allows cross-region replication from ${aws_s3_bucket.this.id} to ${aws_s3_bucket.this_alt.id}"
  policy      = data.aws_iam_policy_document.replication.json
}

data "aws_iam_policy_document" "replication" {
  # allow replication role to use kms keys
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
      for folder in local.objects_to_replicate : [
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
      for folder in local.objects_to_replicate : [
        "${aws_s3_bucket.this_alt.arn}/${folder}",
        "${aws_s3_bucket.this_alt.arn}/${folder}/*",
      ]
    ])
  }
}

# The replication config.
resource "aws_s3_bucket_replication_configuration" "replication" {
  # Must have bucket versioning enabled first
  depends_on = [
    aws_s3_bucket_versioning.this,
    aws_s3_bucket.this_alt,
  ]

  bucket = aws_s3_bucket.this.id
  role   = aws_iam_role.replication.arn

  dynamic "rule" {
    for_each = local.objects_to_replicate

    content {
      id       = rule.value
      priority = rule.key
      status   = "Enabled"

      filter {
        prefix = rule.value
      }

      destination {
        bucket        = aws_s3_bucket.this_alt.arn
        storage_class = local.alt_bucket_storage_class
      }

      delete_marker_replication {
        status = "Enabled"
      }
    }
  }
}
