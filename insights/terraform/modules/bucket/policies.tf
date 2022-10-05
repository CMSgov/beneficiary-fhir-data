## Athena policies

resource "aws_iam_policy" "full" {
  name        = "bfd-insights-full-${var.name}"
  path        = "/bfd-insights/"
  description = "Allow full access and use of the ${var.name} bucket"
  policy      = <<-POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "s3:PutAnalyticsConfiguration",
                "s3:GetObjectVersionTagging",
                "s3:CreateBucket",
                "s3:ReplicateObject",
                "s3:GetObjectAcl",
                "s3:GetBucketObjectLockConfiguration",
                "s3:DeleteBucketWebsite",
                "s3:PutLifecycleConfiguration",
                "s3:GetObjectVersionAcl",
                "s3:PutBucketAcl",
                "s3:PutObjectTagging",
                "s3:HeadBucket",
                "s3:DeleteObject",
                "s3:DeleteObjectTagging",
                "s3:GetBucketPolicyStatus",
                "s3:PutAccountPublicAccessBlock",
                "s3:GetObjectRetention",
                "s3:GetBucketWebsite",
                "s3:PutReplicationConfiguration",
                "s3:DeleteObjectVersionTagging",
                "s3:PutObjectLegalHold",
                "s3:GetObjectLegalHold",
                "s3:GetBucketNotification",
                "s3:PutBucketCORS",
                "s3:DeleteBucketPolicy",
                "s3:GetReplicationConfiguration",
                "s3:ListMultipartUploadParts",
                "s3:PutObject",
                "s3:GetObject",
                "s3:PutBucketNotification",
                "s3:PutBucketLogging",
                "s3:PutObjectVersionAcl",
                "s3:GetAnalyticsConfiguration",
                "s3:PutBucketObjectLockConfiguration",
                "s3:GetObjectVersionForReplication",
                "s3:GetLifecycleConfiguration",
                "s3:GetInventoryConfiguration",
                "s3:GetBucketTagging",
                "s3:PutAccelerateConfiguration",
                "s3:DeleteObjectVersion",
                "s3:GetBucketLogging",
                "s3:ListBucketVersions",
                "s3:ReplicateTags",
                "s3:RestoreObject",
                "s3:ListBucket",
                "s3:GetAccelerateConfiguration",
                "s3:GetBucketPolicy",
                "s3:PutEncryptionConfiguration",
                "s3:GetEncryptionConfiguration",
                "s3:GetObjectVersionTorrent",
                "s3:AbortMultipartUpload",
                "s3:PutBucketTagging",
                "s3:GetBucketRequestPayment",
                "s3:GetObjectTagging",
                "s3:GetMetricsConfiguration",
                "s3:DeleteBucket",
                "s3:PutBucketVersioning",
                "s3:PutObjectAcl",
                "s3:GetBucketPublicAccessBlock",
                "s3:ListBucketMultipartUploads",
                "s3:PutBucketPublicAccessBlock",
                "s3:ListAccessPoints",
                "s3:PutMetricsConfiguration",
                "s3:PutObjectVersionTagging",
                "s3:GetBucketVersioning",
                "s3:GetBucketAcl",
                "s3:BypassGovernanceRetention",
                "s3:PutInventoryConfiguration",
                "s3:GetObjectTorrent",
                "s3:ObjectOwnerOverrideToBucketOwner",
                "s3:GetAccountPublicAccessBlock",
                "s3:PutBucketWebsite",
                "s3:ListAllMyBuckets",
                "s3:PutBucketRequestPayment",
                "s3:PutObjectRetention",
                "s3:GetBucketCORS",
                "s3:PutBucketPolicy",
                "s3:GetBucketLocation",
                "s3:ReplicateDelete",
                "s3:GetObjectVersion"
            ],
            "Resource": [
                "${aws_s3_bucket.main.arn}/*",
                "${aws_s3_bucket.main.arn}"
            ]
        },
        {
          "Sid": "CMK",
          "Effect": "Allow",
          "Action": [
              "kms:Encrypt",
              "kms:Decrypt",
              "kms:ReEncrypt*",
              "kms:GenerateDataKey*",
              "kms:DescribeKey"
          ],
          "Resource": "${aws_kms_key.main.arn}"
        }
    ]
}
POLICY  
}

resource "aws_iam_group_policy_attachment" "full_attach" {
  count       = length(var.full_groups)
  group       = var.full_groups[count.index]
  policy_arn  = aws_iam_policy.full.arn
}

# Allows writes to outputs
resource "aws_iam_policy" "athena_query" {
  name        = "bfd-insights-athena-query-${var.name}"
  path        = "/bfd-insights/"
  description = "Rights needed for athena query access"
  policy      = <<-POLICY
  {
    "Version": "2012-10-17",
    "Statement": [{
      "Sid": "s3QueryPolicy",
      "Effect": "Allow",
      "Action": [
          "s3:GetBucketLocation",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:ListBucketMultipartUploads",
          "s3:ListMultipartUploadParts",
          "s3:AbortMultipartUpload",
          "s3:CreateBucket",
          "s3:PutObject"
      ],
      "Resource": [
          "arn:aws:s3:::aws-athena-query-results-*",
          "${aws_s3_bucket.main.arn}",
          "${aws_s3_bucket.main.arn}/*"
      ]
    },
    {
      "Sid": "CMK",
      "Effect": "Allow",
      "Action": [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
      ],
      "Resource": "${aws_kms_key.main.arn}"
    }]
  } 
  POLICY  
}

resource "aws_iam_group_policy_attachment" "athena_attach" {
  count       = length(var.athena_groups)
  group       = var.athena_groups[count.index]
  policy_arn  = aws_iam_policy.full.arn
}

resource "aws_s3_bucket_policy" "cross_account" {
    count       = length(var.cross_accounts) > 0 ? 1 : 0
    bucket      = aws_s3_bucket.main.id
    policy      = <<-POLICY
    {
      "Version": "2012-10-17",
      "Id": "AccessToDB",
      "Statement": [
          {
              "Sid": "StmtID",
              "Effect": "Allow",
              "Principal": {
                "AWS": [
                  ${join(",", formatlist("\"%s\"", var.cross_accounts))}
                ]
              },
              "Action": [
                  "s3:AbortMultipartUpload",
                  "s3:GetBucketLocation",
                  "s3:ListBucket",
                  "s3:ListBucketMultipartUploads",
                  "s3:*Object"
              ],
              "Resource": [
                  "${aws_s3_bucket.main.arn}/*",
                  "${aws_s3_bucket.main.arn}"
              ]
          },
        {
            "Sid": "AllowSSLRequestsOnly",
            "Effect": "Deny",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": [
                  "${aws_s3_bucket.main.arn}",
                  "${aws_s3_bucket.main.arn}/*"
            ],
            "Condition": {
                "Bool": {
                    "aws:SecureTransport": "false"
                }
            }
        }
      ]
    }
    POLICY
}

## KMS CMK 

data "aws_iam_policy_document" "cmk_policy" {
  statement {
    sid     = "AllowAdmin"
    effect  = "Allow"
    actions = ["kms:*"]
    principals {
      type = "AWS"
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"]
    }
    resources = ["*"]
  }
  
  statement {
    sid = "AllowUse"
    effect = "Allow"
    actions = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
    ]
    resources = ["*"]
    principals {
      type        = "AWS"
      identifiers = var.cross_accounts
    }
  }
}

# Grant to QuickSight access to the key
resource "aws_kms_grant" "cmk_grant" {
  name              = "${local.key_name}-grant"
  key_id            = aws_kms_key.main.key_id
  grantee_principal = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/service-role/aws-quicksight-service-role-v0"
  operations        = ["Encrypt", "Decrypt", "GenerateDataKey", "DescribeKey", "ReEncryptTo", "ReEncryptFrom"]
}

