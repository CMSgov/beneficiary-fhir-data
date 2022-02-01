provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

# AWS account ID
data "aws_caller_identity" "current" {}

# BFD app engineers group
data "aws_iam_group" "app_eng" {
  group_name = "bfd-app-engineers"
}

# BFD devops engineers group
data "aws_iam_group" "devops_eng" {
  group_name = "bfd-admins"
}

# IAM policy and attachment for app eng group param store permissions
resource "aws_iam_policy" "app_eng_parameter_store" {
  name        = "bfd-app-eng-parameter-store"
  description = "IAM policy for application engineer access to parameter store"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ssm:DescribeParameters",
                "ssm:GetParameterHistory",
                "ssm:GetParametersByPath",
                "ssm:GetParameters",
                "ssm:GetParameter"
            ],
            "Resource": [
                "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "ssm:DeleteParameter",
                "ssm:DeleteParameters",
                "ssm:LabelParameterVersion",
                "ssm:PutParameter"
            ],
            "Resource": [
                "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/local/*"
            ]
        }
    ]
}
EOF

}

resource "aws_iam_group_policy_attachment" "app_eng_parameter_store" {
  group      = data.aws_iam_group.app_eng.group_id
  policy_arn = aws_iam_policy.app_eng_parameter_store.arn
}

# IAM policy and attachment for devops eng group param store permissions
resource "aws_iam_policy" "devops_eng_parameter_store" {
  name        = "bfd-admins-parameter-store"
  description = "IAM policy for devops engineer access to parameter store"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ssm:DeleteParameter",
                "ssm:DeleteParameters",
                "ssm:DescribeParameters",
                "ssm:GetParameterHistory",
                "ssm:GetParametersByPath",
                "ssm:GetParameters",
                "ssm:GetParameter",
                "ssm:LabelParameterVersion",
                "ssm:PutParameter"
            ],
            "Resource": [
                "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/*"
            ]
        }
    ]
}
EOF

}

resource "aws_iam_group_policy_attachment" "devops_eng_parameter_store" {
  group      = data.aws_iam_group.devops_eng.group_id
  policy_arn = aws_iam_policy.devops_eng_parameter_store.arn
}
