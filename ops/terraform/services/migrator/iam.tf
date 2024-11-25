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
  name        = "bfd-${local.env}-${local.service}-sqs"
  description = "Permissions to specific SQS queue for ${local.service} in ${local.env}"
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
                  "kms:Decrypt",
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
  name        = "bfd-${local.env}-${local.service}-ssm-parameters"
  description = "Permissions to /bfd/${local.env}/common/nonsensitive, /bfd/${local.env}/${local.service}, /bfd/mgmt/common/sensitive/user SSM hierarchies"
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
            "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/mgmt/common/sensitive/user/*",
            "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/sensitive/user/*",
            "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/sensitive/new_relic/*",
            "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/nonsensitive/*",
            "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/${local.service}/*"
          ]
        },
        {
          "Effect" : "Allow",
          "Action" : [
            "kms:Decrypt"
          ],
          "Resource" : concat(local.mgmt_kms_config_key_arns, local.kms_config_key_arns)
        }
      ]
    }
  )
}

resource "aws_iam_role" "this" {
  name        = "bfd-${local.env}-${local.service}"
  path        = "/"
  description = "Role for instance profile use for ${local.service} in ${local.env}"

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
    data.aws_iam_policy.ec2_instance_tags_ro.arn,
  ]
}

resource "aws_iam_instance_profile" "this" {
  name = "bfd-${local.env}-${local.service}"
  role = aws_iam_role.this.name
}
