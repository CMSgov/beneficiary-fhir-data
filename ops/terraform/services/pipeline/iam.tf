resource "aws_iam_user" "this" {
  count         = local.create_etl_user ? 1 : 0
  force_destroy = false
  name          = "bfd-${local.env}-${local.legacy_service}"
  path          = "/"
  tags = {
    Note    = "NoRotate"
    Purpose = "ETL PUT"
    UsedBy  = "CCW"
  }
}

resource "aws_iam_access_key" "this" {
  count = local.create_etl_user ? 1 : 0
  user  = aws_iam_user.this[0].name
}

resource "aws_iam_group" "this" {
  count = local.create_etl_user ? 1 : 0
  name  = "bfd-${local.env}-${local.legacy_service}"
  path  = "/"
}

resource "aws_iam_group_membership" "this" {
  count = local.create_etl_user ? 1 : 0
  group = aws_iam_group.this[0].name
  name  = "bfd-${local.env}-${local.legacy_service}"
  users = [aws_iam_user.this[0].name]
}

resource "aws_iam_policy" "etl-rw-s3" {
  count       = local.create_etl_user ? 1 : 0
  description = "ETL read-write S3 policy"
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Statement" : [
        {
          "Sid" : "ETLRWKMS",
          "Action" : [
            "kms:Decrypt"
          ],
          "Effect" : "Allow",
          "Resource" : [
            local.kms_key_id
          ]
        },
        {
          "Sid" : "ETLRWBucketList",
          "Action" : [
            "s3:ListBucket"
          ],
          "Effect" : "Allow",
          "Resource" : [
            aws_s3_bucket.this.arn,
            # NOTE: Only included in the prod environment for CCW verification
            # TODO: Remove after CCW Verification is complete, ca Q1 2023.
            "%{if local.is_prod}${aws_s3_bucket.ccw-verification[0].arn}%{endif}"
          ]
        },
        {
          "Sid" : "ETLRWBucketActions",
          "Action" : [
            "s3:GetObject",
            "s3:PutObject"
          ],
          "Effect" : "Allow",
          "Resource" : [
            "${aws_s3_bucket.this.arn}/*",
            # NOTE: Only included in the prod environment for CCW verification
            # TODO: Remove after CCW Verification is complete, ca Q1 2023.
            "%{if local.is_prod}${aws_s3_bucket.ccw-verification[0].arn}/*%{endif}"
          ]
        }
      ]
  })
}

resource "aws_iam_group_policy_attachment" "etl-rw-s3" {
  count      = local.create_etl_user ? 1 : 0
  group      = aws_iam_group.this[0].id
  policy_arn = aws_iam_policy.etl-rw-s3[0].arn
}

resource "aws_iam_policy" "aws_cli" {
  description = "AWS cli privileges for the BFD Pipeline instance role."
  name        = "bfd-${local.service}-${local.env}-cli"
  path        = "/"
  policy      = <<-EOF
{
  "Statement": [
    {
      "Action": [
        "ec2:DescribeAvailabilityZones"
      ],
      "Effect": "Allow",
      "Resource": "*"
    }
  ],
  "Version": "2012-10-17"
}
EOF
}

resource "aws_iam_policy" "bfd_pipeline_rif" {
  description = "Allow the BFD Pipeline application to read-write the S3 bucket with the RIF in it."
  name        = "bfd-${local.env}-${local.service}-rw-s3-rif"
  path        = "/"
  policy      = <<-EOF
{
  "Statement": [
    {
      "Action": [
        "kms:Encrypt",
        "kms:Decrypt",
        "kms:ReEncrypt*",
        "kms:GenerateDataKey*",
        "kms:DescribeKey"
      ],
      "Effect": "Allow",
      "Resource": [
        "${local.kms_key_id}"
      ],
      "Sid": "BFDPipelineRWS3RIFKMS"
    },
    {
      "Action": [
        "s3:ListBucket"
      ],
      "Effect": "Allow",
      "Resource": [
        "${aws_s3_bucket.this.arn}"
      ],
      "Sid": "BFDPipelineRWS3RIFListBucket"
    },
    {
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation",
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Effect": "Allow",
      "Resource": [
        "${aws_s3_bucket.this.arn}/*"
      ],
      "Sid": "BFDPipelineRWS3RIFReadWriteObjects"
    }
  ],
  "Version": "2012-10-17"
}
EOF
}

resource "aws_iam_instance_profile" "this" {
  name = "bfd-${local.env}-bfd_${local.service}-profile"
  path = "/"
  role = aws_iam_role.this.name
}

resource "aws_iam_policy" "ssm" {
  description = "Permissions to /bfd/${local.env}/common/nonsensitive, /bfd/${local.env}/${local.service}, /bfd/mgmt/common/sensitive/user SSM hierarchies"
  name        = "bfd-${local.env}-${local.service}-ssm-parameters"
  path        = "/"
  policy      = <<-EOF
{
  "Statement": [
    {
      "Action": [
        "ssm:GetParametersByPath",
        "ssm:GetParameters",
        "ssm:GetParameter"
      ],
      "Effect": "Allow",
      "Resource": [
        "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/mgmt/common/sensitive/user/*",
        "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/sensitive/user/*",
        "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/nonsensitive/*",
        "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/${local.service}/*"
      ]
    },
    {
      "Action": [
        "kms:Decrypt"
      ],
      "Effect": "Allow",
      "Resource": [
        "${local.kms_key_id}",
        "${local.mgmt_kms_key_arn}"
      ]
    }
  ],
  "Version": "2012-10-17"
}
EOF
}

resource "aws_iam_role" "this" {
  assume_role_policy    = <<-EOF
{
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Sid": ""
    }
  ],
  "Version": "2012-10-17"
}
EOF
  force_detach_policies = false # TODO unsure of how this works with ephmeral environments just yet...
  managed_policy_arns = [
    aws_iam_policy.aws_cli.arn,
    aws_iam_policy.bfd_pipeline_rif.arn,
    aws_iam_policy.ssm.arn,
    "arn:aws:iam::aws:policy/AmazonElasticFileSystemReadOnlyAccess",
    "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy",
    "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess",
  ]
  max_session_duration = 3600
  name                 = "bfd-${local.env}-bfd_${local.service}-role"
  path                 = "/"
}

resource "aws_iam_policy" "etl_s3_rda_paths_rw" {
  name        = "bfd-${local.env}-${local.service}-etl-s3-rda-paths-rw"
  description = "Read and Write objects within RDA paths"
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Statement" : [
        {
          "Sid" : "ETLRWKMS",
          "Action" : [
            "kms:Decrypt",
            "kms:Encypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*",
            "kms:DescribeKey"
          ],
          "Effect" : "Allow",
          "Resource" : [
            local.kms_key_id
          ]
        },
        {
          "Sid" : "ETLRWBucketList",
          "Action" : [
            "s3:ListBucket"
          ],
          "Effect" : "Allow",
          "Resource" : [
            aws_s3_bucket.this.arn
          ]
        },
        {
          "Sid" : "ETLRWBucketActions",
          "Action" : [
            "s3:GetObject",
            "s3:PutObject",
            "s3:DeleteObject"
          ],
          "Effect" : "Allow",
          "Resource" : [
            "${aws_s3_bucket.this.arn}/RDA-Synthetic/*",
            "${aws_s3_bucket.this.arn}/rda_api_messages/*"
          ]
        }
      ]
  })
}

# attach the rda s3 policy to the paca engineer group
data "aws_iam_group" "paca_engineers" {
  group_name = "bfd-paca-app-engineers"
}

resource "aws_iam_group_policy_attachment" "etl_s3_rda_paths_rw" {
  group      = data.aws_iam_group.paca_engineers.group_name
  policy_arn = aws_iam_policy.etl_s3_rda_paths_rw.arn
}
