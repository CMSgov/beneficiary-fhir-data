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
            "route53:ListHostedZones",
            "s3:ListAllMyBuckets",
            "sts:GetCallerIdentity"
          ]
          Resource = "*"
        },
        {
          Sid    = "AllowSsmAccess"
          Effect = "Allow"
          Action = [
            "ssm:Describe*",
            "ssm:GetParametersByPath",
            "ssm:GetParameter",
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
            "kms:GenerateDataKey*",
            "kms:DescribeKey",
            "kms:GetKey*",
            "kms:List*"
          ]
          Resource = concat(local.all_kms_config_key_arns, local.all_kms_data_key_arns)
        },
        {
          Sid    = "AllowListOfAllKeys"
          Effect = "Allow"
          Action = [
            "kms:List*"
          ]
          Resource = "*"
        }
      ]
      Version = "2012-10-17"
    }
  )
}
