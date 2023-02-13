data "aws_caller_identity" "current" {}

locals {
  service = var.name == "fhir" ? "server" : "pipeline" # NOTE: with this, the iam module is only capable of supporting the server and pipeline services
}

data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${var.env_config.env}-cmk"
}

data "aws_iam_policy" "cloudwatch_agent_policy" {
  arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# AWS CloudWatch agent needs extra IAM permissions for x-ray
data "aws_iam_policy" "cloudwatch_xray_policy" {
  arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_iam_instance_profile" "instance" {
  name = "bfd-${var.env_config.env}-${var.name}-profile"
  role = aws_iam_role.instance.name
}

# EC2 instance role
resource "aws_iam_role" "instance" {
  name = "bfd-${var.env_config.env}-${var.name}-role"
  path = "/"

  assume_role_policy = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Principal": {
          "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow",
        "Sid": ""
      }
    ]
  }
  EOF
}

# policy to allow full s3 privs
resource "aws_iam_role_policy" "s3_policy" {
  count = length(var.s3_bucket_arns) > 0 ? 1 : 0
  name  = "bfd-${var.env_config.env}-${var.name}-s3-policy"
  role  = aws_iam_role.instance.id

  policy = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      %{for arn in var.s3_bucket_arns}
      {
        "Action": "s3:*",
        "Effect": "Allow",
        "Resource": ["${arn}/*"]
      }
      %{endfor}
    ]
  }
  EOF
}

# attach AWS managed CloudWatchAgentServerPolicy to all EC2 instances
resource "aws_iam_role_policy_attachment" "cloudwatch_agent_policy_attachment" {
  role       = aws_iam_role.instance.id
  policy_arn = data.aws_iam_policy.cloudwatch_agent_policy.arn
}

# attach AWS managed AWSXRayDaemonWriteAccess to all EC2 instances
resource "aws_iam_role_policy_attachment" "cloudwatch_xray_policy" {
  role       = aws_iam_role.instance.id
  policy_arn = data.aws_iam_policy.cloudwatch_xray_policy.arn
}

# TODO: Separate SSM and KMS statements
resource "aws_iam_policy" "ssm" {
  name        = "bfd-${var.env_config.env}-${local.service}-ssm-parameters"
  description = "Permissions to /bfd/${var.env_config.env}/common/nonsensitive, /bfd/${var.env_config.env}/${local.service} SSM hierarchies"
  policy      = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParametersByPath",
        "ssm:GetParameters",
        "ssm:GetParameter"
      ],
      "Resource": [

        "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/${var.env_config.env}/common/nonsensitive/*",
        "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/${var.env_config.env}/${local.service}/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:GenerateDataKey",
        "kms:ReEncrypt",
        "kms:DescribeKey",
        "kms:CreateGrant",
        "kms:ListGrants",
        "kms:RevokeGrant"
      ],
      "Resource": [
        "${data.aws_kms_key.master_key.arn}"
      ]
    }
  ]
}
EOF
}

# attach AWS managed SSM parameters to all EC2 instances
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.instance.id
  policy_arn = aws_iam_policy.ssm.arn
}
