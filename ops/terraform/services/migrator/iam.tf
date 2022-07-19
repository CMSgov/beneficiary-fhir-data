#AWS managed Cloudwatch Agent Policy
# TODO: consider refactoring, hoisting IAM resources into a centrally managed environmental configuration for services
data "aws_iam_policy" "cloudwatch_agent_policy" {
  arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

data "aws_iam_policy" "ansible_vault_ro" {
  arn = "arn:aws:iam::${local.account_id}:policy/bfd-ansible-vault-pw-ro-s3"
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
          }
      ]
  }
  EOF
}

resource "aws_iam_policy" "ssm" {
  name        = "bfd-${local.env}-${local.service}-ssm-parameters"
  description = "Permissions to /bfd/${local.env}/common/nonsensitive, /bfd/${local.env}/${local.service} SSM hierarchies"
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
                "${data.aws_kms_key.main.arn}"
            ]
        }
    ]
}
EOF
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
    data.aws_iam_policy.ansible_vault_ro.arn,
    aws_iam_policy.sqs.arn,
    aws_iam_policy.ssm.arn,
  ]
}

resource "aws_iam_instance_profile" "this" {
  name = "bfd-${local.env}-${local.service}"
  role = aws_iam_role.this.name
}
