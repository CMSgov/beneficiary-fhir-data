locals {
  env = terraform.workspace
}

resource "aws_iam_instance_profile" "instance" {
  name = "bfd-${local.env}-${var.legacy_service}-profile"
  role = aws_iam_role.instance.name
}

# EC2 instance role
resource "aws_iam_role" "instance" {
  name = "bfd-${local.env}-${var.legacy_service}-role"
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
      },
      {
        "Action": "sts:AssumeRole",
        "Effect": "Allow",
        "Principal": {
          "Service": "ssm.amazonaws.com"
        },
        "Sid": ""
      }
    ]
  }
  EOF
}

resource "aws_iam_role_policy_attachment" "ssm_service_role_attachment" {
  role       = aws_iam_role.instance.id
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
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
  name        = "bfd-${local.env}-${var.service}-ssm-parameters"
  description = "Permissions to /bfd/${local.env}/common/nonsensitive, /bfd/${local.env}/${var.service} SSM hierarchies"
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
        "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/bfd/${local.env}/common/sensitive/user/*",
        "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/bfd/${local.env}/common/nonsensitive/*",
        "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/bfd/${local.env}/${var.service}/*"
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

resource "aws_iam_policy" "ssm_mgmt" {
  description = "Policy granting BFD Server in ${local.env} environment access to certain mgmt SSM hierarchies"
  name        = "bfd-${local.env}-${var.service}-ssm-mgmt-parameters"
  path        = "/"
  policy      = <<-POLICY
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
        "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/bfd/mgmt/common/sensitive/user/*"
      ],
      "Sid": "BFDProfile"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
}

# attach AWS managed SSM mgmt parameters to all EC2 instances
resource "aws_iam_role_policy_attachment" "ssm_mgmt" {
  role       = aws_iam_role.instance.id
  policy_arn = aws_iam_policy.ssm_mgmt.arn
}

resource "aws_iam_policy" "kms_mgmt" {
  description = "Policy granting BFD Server in ${local.env} environment access to decrypt using the mgmt KMS key"
  name        = "bfd-${local.env}-${var.service}-kms-mgmt"
  path        = "/"
  policy      = <<-POLICY
{
  "Statement": [
    {
      "Action": ["kms:Decrypt"],
      "Effect": "Allow",
      "Resource": [
        "${data.aws_kms_key.mgmt_key.arn}"
      ]
    }
  ],
  "Version": "2012-10-17"
}
POLICY
}

# attach policy allowing BFD Server to decrypt using mgmt KMS to all EC2 instances
resource "aws_iam_role_policy_attachment" "kms_mgmt" {
  role       = aws_iam_role.instance.id
  policy_arn = aws_iam_policy.kms_mgmt.arn
}

# Allow instances to generate RDS auth tokens
data "aws_iam_policy_document" "rds" {
  statement {
    sid    = "AllowRdsIamAuth"
    effect = "Allow"
    actions = ["rds-db:connect"]
    resources = [
      "arn:aws:rds-db:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:dbuser:${var.rds_cluster_resource_id}/${aws_iam_role.instance.name}"
    ]
  }
}

resource "aws_iam_policy" "rds" {
  name        = "bfd-${local.env}-${var.service}-rds-auth"
  description = "IAM policy to allow instances to generate RDS auth tokens"
  policy      = data.aws_iam_policy_document.rds.json
}

resource "aws_iam_role_policy_attachment" "rds-policy-attach" {
  role       = aws_iam_role.instance.id
  policy_arn = aws_iam_policy.rds.arn
}
