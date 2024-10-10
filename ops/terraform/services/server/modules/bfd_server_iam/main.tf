locals {
  region     = data.aws_region.current.name
  account_id = data.aws_caller_identity.current.account_id
  env        = terraform.workspace

  kms_config_key_arns = flatten(
    [
      for v in data.aws_kms_key.config_key.multi_region_configuration :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
  mgmt_kms_config_key_arns = flatten(
    [
      for v in data.aws_kms_key.mgmt_config_key.multi_region_configuration :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
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
      }
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

resource "aws_iam_policy" "kms" {
  name        = "bfd-${local.env}-${var.service}-kms"
  description = "Permissions to use the default environment KMS key"
  policy      = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
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

resource "aws_iam_role_policy_attachment" "kms" {
  role       = aws_iam_role.instance.id
  policy_arn = aws_iam_policy.kms.arn
}

resource "aws_iam_policy" "ssm" {
  name        = "bfd-${local.env}-${var.service}-ssm-parameters"
  description = "Permissions to /bfd/${local.env}/common/nonsensitive, /bfd/${local.env}/${var.service} SSM hierarchies"
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Statement" : [
        {
          "Effect" : "Allow",
          "Action" : [
            "ssm:GetParametersByPath",
            "ssm:GetParameters",
            "ssm:GetParameter"
          ],
          "Resource" : [
            "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/${local.env}/common/sensitive/user/*",
            "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/${local.env}/common/nonsensitive/*",
            "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/${local.env}/common/sensitive/new_relic/*",
            "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/${local.env}/${var.service}/*"
          ]
        },
        {
          "Effect" : "Allow",
          "Action" : [
            "kms:Decrypt"
          ],
          "Resource" : local.kms_config_key_arns
        }
      ]
    }
  )
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
        "arn:aws:ssm:us-east-1:${data.aws_caller_identity.current.account_id}:parameter/bfd/mgmt/common/sensitive/user/*"
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
  description = "Policy granting BFD Server in ${local.env} environment access to decrypt using the mgmt KMS keys"
  name        = "bfd-${local.env}-${var.service}-kms-mgmt"
  path        = "/"
  policy = jsonencode(
    {
      "Statement" : [
        {
          "Action" : ["kms:Decrypt"],
          "Effect" : "Allow",
          "Resource" : concat(
            ["${data.aws_kms_key.mgmt_key.arn}"],
            local.mgmt_kms_config_key_arns
          )
        }
      ],
      "Version" : "2012-10-17"
    }
  )
}

# attach policy allowing BFD Server to decrypt using mgmt KMS to all EC2 instances
resource "aws_iam_role_policy_attachment" "kms_mgmt" {
  role       = aws_iam_role.instance.id
  policy_arn = aws_iam_policy.kms_mgmt.arn
}


# allow Server instances to complete lifecycle actions on their ASG
resource "aws_iam_policy" "asg" {
  description = join("", [
    "Policy granting BFD Server in ${local.env} environment access to complete Lifecycle Actions ",
    "on the ${local.env} AutoScaling Group"
  ])
  name = "bfd-${local.env}-${var.service}-asg"
  path = "/"
  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "autoscaling:CompleteLifecycleAction"
          Resource = ["arn:aws:autoscaling:${local.region}:${local.account_id}:autoScalingGroup:*:autoScalingGroupName/bfd-${local.env}-${var.legacy_service}*"]
        }
      ]
    }
  )
}

# attach policy allowing BFD Server to complete lifecycle actions on its ASG
resource "aws_iam_role_policy_attachment" "asg" {
  role       = aws_iam_role.instance.id
  policy_arn = aws_iam_policy.asg.arn
}
