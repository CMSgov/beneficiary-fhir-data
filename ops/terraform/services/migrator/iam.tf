#AWS managed Cloudwatch Agent Policy
# TODO: consider refactoring, hoisting IAM resources into a centrally managed environmental configuration for services
data "aws_iam_policy" "cloudwatch_agent_policy" {
  arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# AWS CloudWatch agent needs extra IAM permissions for x-ray
data "aws_iam_policy" "cloudwatch_agent_xray_policy" {
  arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_iam_policy" "sqs" {
  name        = "${local.stack}-sqs"
  description = "SQS perms for ${local.stack}"
  policy      = <<-EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Action": [
                  "sqs:GetQueueUrl",
                  "sqs:SendMessage"
              ],
              "Resource": "${aws_sqs_queue.this.arn}"
          },
          {
              "Effect": "Allow",
              "Action": [
                  "kms:Encrypt",
                  "kms:GenerateDataKey"
              ],
              "Resource": [
                  "${local.kms_key_arn}"
              ]
          }
      ]
  }
  EOF
}

resource "aws_iam_policy" "ssm" {
  name        = "${local.stack}-ssm-parameters"
  description = "SSM perms for ${local.stack}"
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
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/mgmt/common/sensitive/user/*",
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/sensitive/user/*",
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/nonsensitive/*",
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/${local.service}/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "kms:Decrypt"
            ],
            "Resource": [
                "${local.kms_key_arn}",
                "${local.mgmt_kms_key_arn}"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_role" "this" {
  name        = local.stack
  path        = "/"
  description = "Instance profile role for ${local.stack}"

  assume_role_policy = <<-EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Action": "sts:AssumeRole",
              "Effect": "Allow",
              "Principal": {
                  "Service": "ec2.amazonaws.com"
              }
          }
      ]
  }
  EOF
  managed_policy_arns = [
    data.aws_iam_policy.cloudwatch_agent_policy.arn,
    data.aws_iam_policy.cloudwatch_agent_xray_policy.arn,
    aws_iam_policy.sqs.arn,
    aws_iam_policy.ssm.arn,
  ]
}

resource "aws_iam_instance_profile" "this" {
  name = local.stack
  role = aws_iam_role.this.name
}
