resource "aws_iam_policy" "github_actions_s3its" {
  description = "GitHub Actions policy for S3 integration tests"
  name        = "bfd-${local.env}-github-actions-s3its"
  path        = "/"
  policy      = <<-POLICY
{
  "Statement": [
    {
      "Action": [
        "s3:CreateBucket",
        "s3:ListAllMyBuckets"
      ],
      "Effect": "Allow",
      "Resource": "*",
      "Sid": "BFDGitHubActionsS3ITs"
    },
    {
      "Action": [
        "s3:DeleteBucket",
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::bb-test-*",
      "Sid": "BFDGitHubActionsS3ITsBucket"
    },
    {
      "Action": [
        "s3:DeleteObject",
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::bb-test-*/*",
      "Sid": "BFDGitHubActionsS3ITsObject"
    },
    {
        "Sid": "BFDGitHubActionsS3ITsBucketSecurity",
        "Effect": "Allow",
        "Action": [
            "s3:PutEncryptionConfiguration",
            "s3:PutBucketPublicAccessBlock",
            "s3:PutBucketAcl",
            "s3:PutBucketPolicy"
        ],
        "Resource": "arn:aws:s3:::bb-test-*"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
}

resource "aws_iam_policy" "github_actions_ecr" {
  name = "bfd-${local.env}-ecr-rw"
  path = "/"
  policy = jsonencode(
    {
      Statement = [
        {
          Action = [
            "ecr:GetAuthorizationToken",
            "ecr-public:GetAuthorizationToken"
          ]
          Effect   = "Allow"
          Resource = "*"
          Sid      = "GetAuthorizationToken"
        },
        {
          Action = [
            "ecr:BatchGetImage",
            "ecr:BatchCheckLayerAvailability",
            "ecr:CompleteLayerUpload",
            "ecr:GetDownloadUrlForLayer",
            "ecr:InitiateLayerUpload",
            "ecr:PutImage",
            "ecr:UploadLayerPart",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:ecr:us-east-1:${local.account_id}:repository/bfd-db-migrator",
            "arn:aws:ecr:us-east-1:${local.account_id}:repository/bfd-server",
            "arn:aws:ecr:us-east-1:${local.account_id}:repository/bfd-pipeline-app",
            "arn:aws:ecr:us-east-1:${local.account_id}:repository/bfd-mgmt-eft-sftp-outbound-transfer-lambda",
          ]
          Sid = "AllowPushPull"
        },
        {
          Sid    = "AllowDescribeRegistry",
          Effect = "Allow",
          Action = [
            "ecr:DescribeRegistry"
          ],
          Resource = [
            "*"
          ]
        }
      ]
      Version = "2012-10-17"
    }
  )
}

resource "aws_iam_policy" "github_actions_tf_state" {
  name        = "bfd-${local.env}-gha-tf-state"
  description = "Grants permissions necessary for GHA to modify/read Terraform state"
  path        = "/"
  policy = jsonencode(
    {
      Statement = [
        {
          Sid    = "AllowTerraformDynamoStateManagement"
          Effect = "Allow"
          Action = [
            "dynamodb:DeleteItem",
            "dynamodb:GetItem",
            "dynamodb:PutItem"
          ]
          Resource = "arn:aws:dynamodb:${local.region}:${local.account_id}:table/bfd-tf-table"
        },
        {
          Sid    = "AllowTerraformS3StateManagement"
          Effect = "Allow"
          Action = [
            "s3:ListBucket",
            "s3:GetObject*",
            "s3:PutObject*"
          ]
          Resource = ["arn:aws:s3:::bfd-tf-state", "arn:aws:s3:::bfd-tf-state/*"]
        },
        {
          Sid    = "AllowDescribeAndUseOfStateCMK"
          Effect = "Allow"
          Action = [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*",
            "kms:DescribeKey"
          ]
          Resource = data.aws_kms_key.tf_state.arn
        },
      ]
      Version = "2012-10-17"
    }
  )
}

resource "aws_iam_policy" "github_actions_tf_logs" {
  name        = "bfd-${local.env}-gha-tf-logs"
  description = "Grants permissions necessary for GHA to submit Terraform logs to CloudWatch Logs"
  path        = "/"
  policy = jsonencode(
    {
      Statement = [
        {
          Sid    = "AllowCloudWatchLogsDescribeActions"
          Effect = "Allow"
          Action = [
            "logs:DescribeLogGroups",
            "logs:DescribeLogStreams"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowCloudWatchLogStreamActions"
          Effect = "Allow"
          Action = [
            "logs:CreateLogStream",
            "logs:PutLogEvents"
          ]
          Resource = [
            "arn:aws:logs:${local.region}:${local.account_id}:log-group:*:log-stream:*"
          ]
        },
        {
          Sid    = "AllowDescribeAndEncryptWithDataCMKs"
          Effect = "Allow"
          Action = [
            "kms:Encrypt",
            "kms:GenerateDataKey*",
            "kms:DescribeKey"
          ]
          Resource = local.all_kms_data_key_arns
        }
      ]
      Version = "2012-10-17"
    }
  )
}

# FUTURE: When IAM Role-based permissions are introduced, refactor this policy out into a generic resource
resource "aws_iam_policy" "github_actions_static_site" {
  name = "bfd-${local.env}-static-site"
  description = join("", [
    "Grants permissions necessary to apply the static-site Terraservice and manipulate the Static",
    " Site artifacts in any environment"
  ])
  path = "/"
  policy = jsonencode(
    {
      Statement = [
        {
          Sid = "AllowAllCloudfrontActions"
          Action = [
            "cloudfront:*"
          ]
          Effect   = "Allow"
          Resource = "*"
        },
        {
          Sid    = "AllowManagementOfStaticSiteBuckets"
          Effect = "Allow"
          Action = [
            "s3:CreateBucket*",
            "s3:PutBucket*",
            "s3:GetBucket*",
            "s3:ListBucket*",
            "s3:DeleteBucket*",
            "s3:*LifecycleConfiguration",
            "s3:*EncryptionConfiguration",
            "s3:*AccelerateConfiguration",
            "s3:*ReplicationConfiguration"
          ]
          Resource = ["arn:aws:s3:::bfd-*-static", "arn:aws:s3:::bfd-*-staticlogging"]
        },
        {
          Sid    = "AllowFullControlOfStaticSiteBucketObjects"
          Effect = "Allow"
          Action = [
            "s3:CreateObject*",
            "s3:PutObject*",
            "s3:GetObject*",
            "s3:ListObject*",
            "s3:DeleteObject*",
            "s3:AbortMultipartUpload",
            "s3:ListMultipartUploadParts"
          ]
          Resource = ["arn:aws:s3:::bfd-*-static/*", "arn:aws:s3:::bfd-*-staticlogging/*"]
        },
        {
          Sid    = "AllowUnconditionalDescribeActions"
          Effect = "Allow"
          Action = [
            "ec2:DescribeManagedPrefixLists",
            "ec2:DescribeRouteTables",
            "ec2:DescribeVpcs",
            "route53:ListHostedZones",
            "s3:ListAllMyBuckets",
            "sts:GetCallerIdentity"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowGetHostedZoneParams"
          Effect = "Allow"
          Action = [
            "ssm:GetParameter"
          ]
          Resource = [
            "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/mgmt/common/sensitive/r53_hosted_zone_root_domain",
            "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/mgmt/common/sensitive/r53_hosted_zone_root_is_private"
          ]
        },
        {
          Sid    = "AllowGetStaticSiteAndCommonParams"
          Effect = "Allow"
          Action = [
            "ssm:GetParametersByPath"
          ]
          Resource = [
            "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/*/static-site*",
            "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/*/common*"
          ]
        },
        {
          Sid    = "AllowGetVpnPrefixList"
          Effect = "Allow"
          Action = [
            "ec2:GetManagedPrefixListEntries"
          ]
          Resource = data.aws_ec2_managed_prefix_list.vpn.arn
        },
        {
          Sid    = "AllowGetRootHostedZone"
          Effect = "Allow"
          Action = [
            "route53:GetHostedZone"
          ]
          Resource = aws_route53_zone.zones["root"].arn
        },
        {
          Sid    = "AllowListingRoute53ResourceRecordSets"
          Effect = "Allow"
          Action = [
            "route53:ListResourceRecordSets"
          ]
          Resource = "arn:aws:route53:::hostedzone/*"
        },
        {
          Sid    = "AllowListingRoute53Tags"
          Effect = "Allow"
          Action = [
            "route53:ListTagsForResource"
          ]
          Resource = [
            "arn:aws:route53:::healthcheck/*",
            "arn:aws:route53:::hostedzone/*"
          ]
        },
        {
          Sid    = "AllowDescribeVpcAttributes"
          Effect = "Allow"
          Action = [
            "ec2:DescribeVpcAttribute"
          ]
          Resource = "arn:aws:ec2:${local.region}:${local.account_id}:vpc/*"
        },
        {
          Sid    = "AllowDescribeAndUseOfCMKs"
          Effect = "Allow"
          Action = [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*",
            "kms:DescribeKey"
          ]
          Resource = concat(local.all_kms_config_key_arns, local.all_kms_data_key_arns)
        }
      ]
      Version = "2012-10-17"
    }
  )
}

# TODO: BFD-3647 for fine-grained adjustment of policy
resource "aws_iam_policy" "github_actions_ci_ops" {
  name        = "bfd-${local.env}-ci-ops-infra"
  description = "Grants permissions necessary to allow CI/CD Pipeline for MGMT baseline config"
  path        = "/"
  policy = jsonencode(
    {
      Statement = [
        {
          Sid    = "AllowUnconditionalDescribeGetListActions"
          Effect = "Allow"
          Action = [
            "ec2:DescribeManagedPrefixLists",
            "ec2:DescribeRouteTables",
            "ec2:DescribeVpcs",
            "ec2:GetEbsEncryptionByDefault",
            "ec2:GetEbsDefaultKmsKeyId",
            "ec2:GetManagedPrefixListEntries",
            "s3:List*",
            "s3:Get*",
            "sts:GetCallerIdentity"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowSsmAccess"
          Effect = "Allow"
          Action = [
            "ssm:ListTagsForResource",
            "ssm:DescribeParameters",
            "ssm:PutParameter",
            "ssm:DeleteParameter",
            "ssm:GetParameterHistory",
            "ssm:GetParametersByPath",
            "ssm:GetParameters",
            "ssm:GetParameter",
            "ssm:DeleteParameters"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowDescribeVpcAttributes"
          Effect = "Allow"
          Action = [
            "ec2:DescribeVpcAttribute"
          ]
          Resource = "arn:aws:ec2:${local.region}:${local.account_id}:vpc/*"
        },
        {
          Sid    = "AllowDescribeAndUseOfCMKs"
          Effect = "Allow"
          Action = [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*"
          ]
          Resource = concat(local.all_kms_config_key_arns, local.all_kms_data_key_arns)
        },
        {
          Sid    = "AllowListOfAllKeys"
          Effect = "Allow"
          Action = [
            "kms:Describe*",
            "kms:GetKey*",
            "kms:List*"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowPolicyManagementOfAllKeys"
          Effect = "Allow"
          Action = [
            "kms:PutKeyPolicy",
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowSNS"
          Effect = "Allow"
          Action = [
            "sns:Get*",
            "sns:List*"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowLogMetrics"
          Effect = "Allow"
          Action = [
            "logs:Describe*",
            "logs:List*"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowIam"
          Effect = "Allow"
          Action = [
            "iam:Get*",
            "iam:List*",
            "iam:DeletePolicyVersion",
            "iam:CreatePolicyVersion"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowCodeArtifact"
          Effect = "Allow"
          Action = [
            "codeartifact:Describe*",
            "codeartifact:Get*",
            "codeartifact:List*",
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowDynamo"
          Effect = "Allow"
          Action = [
            "dynamodb:Describe*",
            "dynamodb:List*"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowQuicksight"
          Effect = "Allow"
          Action = [
            "quicksight:Get*",
            "quicksight:Describe*",
            "quicksight:List*",
            "quicksight:Create*"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowR53"
          Effect = "Allow"
          Action = [
            "route53:GetHostedZone",
            "route53:ListHostedZones",
            "route53:ListResourceRecordSets"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowCloudwatch"
          Effect = "Allow"
          Action = [
            "cloudwatch:Get*",
            "cloudwatch:Describe*",
            "cloudwatch:List*"
          ]
          Resource = "*"
        }
      ]
      Version = "2012-10-17"
    }
  )
}

data "tls_certificate" "github_actions" {
  url = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = data.tls_certificate.github_actions.certificates[*].sha1_fingerprint
  url             = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_role" "github_actions" {
  name        = "bfd-${local.env}-github-actions"
  path        = "/"
  description = "OIDC Assumable GitHub Actions Role"

  managed_policy_arns = [
    aws_iam_policy.code_artifact_rw.arn,
    aws_iam_policy.github_actions_s3its.arn,
    aws_iam_policy.github_actions_ecr.arn,
    aws_iam_policy.github_actions_tf_state.arn,
    aws_iam_policy.github_actions_tf_logs.arn,
    aws_iam_policy.github_actions_static_site.arn,
    aws_iam_policy.github_actions_ci_ops.arn
  ]

  assume_role_policy = jsonencode(
    {
      "Statement" : [
        {
          "Action" : "sts:AssumeRoleWithWebIdentity",
          "Condition" : {
            "ForAllValues:StringLike" : {
              "token.actions.githubusercontent.com:sub" : "repo:CMSgov/beneficiary-fhir-data:*",
              "token.actions.githubusercontent.com:aud" : "sts.amazonaws.com"
            }
          },
          "Effect" : "Allow",
          "Principal" : {
            "Federated" : aws_iam_openid_connect_provider.github_actions.arn
          }
        }
      ],
      "Version" : "2012-10-17"
  })
}
