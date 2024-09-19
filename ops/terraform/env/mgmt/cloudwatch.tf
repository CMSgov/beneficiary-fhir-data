# BFD-3384
# CloudWatch Log Group for the "CI - Update OPS Infrastructure" Terraform plan/apply logs
resource "aws_cloudwatch_log_group" "gha_ci_ops_infra" {
  name       = "/bfd/${local.env}/gha/ci-ops-infra"
  kms_key_id = data.aws_kms_key.cmk.arn
}

resource "aws_iam_policy" "github_actions_ci_ops" {
  name        = "bfd-${local.env}-ci-ops-infra"
  description = "Grants permissions necessary to allow CI/CD PIpeline for MGMT baseline config"
  path        = "/"
  policy = jsonencode(
    {
      Statement = [
        {
          Sid    = "AllowUnconditionalDescribeActions"
          Effect = "Allow"
          Action = [
            "ec2:DescribeManagedPrefixLists",
            "ec2:DescribeRouteTables",
            "ec2:DescribeVpcs",
            "ec2:GetEbsEncryptionByDefault",
            "ec2:GetEbsDefaultKmsKeyId",
            "ec2:GetManagedPrefixListEntries",
            "route53:ListHostedZones",
            "s3:ListAllMyBuckets",
            "s3:Get*",
            "sts:GetCallerIdentity"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowSsmAccess"
          Effect = "Allow"
          Action = [
            "ssm:Describe*",
            "ssm:GetParam*",
            "ssm:List*"
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
            "iam:Describe*"
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
          Sid    = "AllowQuicksignt"
          Effect = "Allow"
          Action = [
            "quicksight:Get*",
            "quicksight:Describe*",
            "quicksight:List*"
          ]
          Resource = "*"
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
        }
      ]
      Version = "2012-10-17"
    }
  )
}
