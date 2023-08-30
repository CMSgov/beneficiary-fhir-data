locals {
  # list the actions developers are allowed to perform on new or existing resources owned by the project
  allowed_actions = [
    "s3:*",
    "lambda:*",
    "glue:*",
    "athena:*",
    "cloudwatch:*"
  ]

  # list the actions developers are not allowed to perform on existing resources owned by this project
  denied_actions = [
    "s3:PutBucketTagging",
    "lambda:TagResource",
    "glue:TagResource",
    "athena:TagResource",
    "cloudwatch:TagResource",
  ]

  # Note: The `RequestTag` checks for tags in the current request (ie, the tags a user is trying to add or modify on a
  # resource), while the `ResourceTag` checks for tags on an existing resource. The following simply maps this project's
  # default tags to the appropriate format for each condition. The `Null` condition is used to ensure that all of the
  # tags in the request/resource are present. This was done so we can easily modify the set of default tags we are
  # required to set application wide, as is expected to happen in the near future.
  request_default_tags_map = merge(
    { for k, v in local.default_tags : "aws:RequestTag/${k}" => v }
  )
  request_default_tags_no_null = merge(
    { for k, _ in local.default_tags : "aws:RequestTag/${k}" => false }
  )
  resource_default_tags_map = merge(
    { for k, v in local.default_tags : "aws:ResourceTag/${k}" => v }
  )
  resource_default_tags_no_null = merge(
    { for k, _ in local.default_tags : "aws:ResourceTag/${k}" => false }
  )
}

# Role BCDA Insights engineers will assume to manage their resources
# TODO: Consider modifying the trust policy to allow for cross-account assumption instead of requiring IAM users in our
# account.
resource "aws_iam_role" "dev" {
  name = "bfd-insights-bcda-developer"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "sts:RoleSessionName" = "$${aws:username}"
          }
        }
      }
    ]
  })

}

# Policy allowing allowed actions on tagged resources and denying denied actions on untagged resources
resource "aws_iam_policy" "dev" {
  name        = "bfd-insights-bcda-developer-policy"
  description = "ABAC policy allowing developers to maintain BCDA related Insights resources."

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Sid      = "AllowOnTaggedResources",
        Effect   = "Allow",
        Action   = local.allowed_actions,
        Resource = "*",
        Condition = {
          StringEqualsIfExists = local.request_default_tags_map,
          Null                 = local.request_default_tags_no_null
        }
      },
      {
        Sid      = "DenyOnUntaggedResources",
        Effect   = "Deny",
        Action   = local.denied_actions,
        Resource = "*",
        Condition = {
          StringNotEqualsIfExists = local.resource_default_tags_map,
          Null                    = local.resource_default_tags_no_null
        }
      }
    ]
  })
}
resource "aws_iam_role_policy_attachment" "dev" {
  role       = aws_iam_role.dev.name
  policy_arn = aws_iam_policy.dev.arn
}

resource "aws_iam_policy" "terraform" {
  name        = "bfd-insights-bcda-terraform-policy"
  description = "Allow BCDA developers to apply terraform changes to the BCDA Insights project."

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Sid    = "AllowTfstateBucketAccess",
        Effect = "Allow",
        Action = [
          "s3:ListBucket",
          "s3:GetObject",
          "s3:PutObject"
        ],
        Resource = [
          "arn:aws:s3:::bfd-tf-state",
          "arn:aws:s3:::bfd-tf-state/bfd-insights/bcda/terraform.tfstate"
        ]
      },
      {
        Sid    = "AllowTfstateLockTableAccess",
        Effect = "Allow",
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:DeleteItem"
        ],
        Resource = "arn:aws:dynamodb:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:table/bfd-tf-table"
      },{
        Sid = "AllowReadingSensitiveParameters",
        Effect = "Allow",
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters",
          "ssm:GetParametersByPath"
        ],
        Resource = "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/bcda/global/sensitive/insights/*"
      }
    ]
  })
}
resource "aws_iam_role_policy_attachment" "terraform" {
  role       = aws_iam_role.dev.name
  policy_arn = aws_iam_policy.terraform.arn
}

# Group specifying which users may assume the developer role
resource "aws_iam_group" "dev" {
  name = "bfd-insights-bcda-developers"
}

resource "aws_iam_group_policy" "this" {
  name  = "bfd-insights-bcda-developers-assume-role"
  group = aws_iam_group.dev.name

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action   = "sts:AssumeRole",
        Resource = aws_iam_role.dev.arn,
        Effect   = "Allow"
      }
    ]
  })
}
