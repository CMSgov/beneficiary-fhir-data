resource "aws_iam_policy" "full" {
  name        = "bfd-insights-full-${var.sensitivity}"
  path        = "/bfd-insights/"
  description = "Allow access and use of the ${var.sensitivity} bucket"
  policy      = <<-EOF
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
        }
    ]
}
EOF  
}

# Allows writes to outputs
resource "aws_iam_policy" "athena_query" {
  name        = "bfd-insights-athena-query-${var.sensitivity}"
  path        = "/bfd-insights/"
  description = "Allow access and use of the customer managed key"
  policy      = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": {
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
    }
  } 
  EOF  
}

resource "aws_iam_policy" "read" {
  name        = "bfd-insights-read-${var.sensitivity}"
  path        = "/bfd-insights/"
  description = "Allow access and use of the customer managed key"
  policy      = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": {
      "Sid": "s3ReadPolicy",
      "Effect": "Allow",
      "Action": [
          "s3:GetBucketLocation",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:HeadBucket",
          "s3:ListBucketMultipartUploads",
          "s3:ListMultipartUploadParts"
      ],
      "Resource": [
          "${aws_s3_bucket.main.arn}",
          "${aws_s3_bucket.main.arn}/*"
      ]
    }
  }
  EOF  
}