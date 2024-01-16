# TODO: Further separate the concerns and provide targeted permissions for Jenkins
resource "aws_iam_policy" "jenkins_permission_boundary" {
  description = "Jenkins Permission Boundary Policy"
  name        = "bfd-${local.env}-jenkins-permission-boundary"
  path        = "/"
  policy = jsonencode(
    {
      "Statement" : [
        {
          "Action" : [
            "rds:*",
            "ec2:Reset*",
            "cloudtrail:GetTrailStatus",
            "logs:*",
            "dynamodb:*",
            "autoscaling:*",
            "iam:GetPolicy*",
            "rds:Describe*",
            "cloudtrail:ListTags",
            "ec2:DeleteNetworkAcl*",
            "config:*",
            "iam:GetServiceLastAccessed*",
            "events:*",
            "ec2:Associate*",
            "sns:*",
            "cloudtrail:LookupEvents",
            "iam:GetRole",
            "iam:GetGroup*",
            "ecr-public:*",
            "ec2:Cancel*",
            "cloudtrail:DescribeTrails",
            "iam:*",
            "ec2:Modify*",
            "cloudwatch:*",
            "ec2:*",
            "waf-regional:*",
            "iam:GetAccount*",
            "ec2:AssignPrivateIpAddresses*",
            "iam:GetUser*",
            "cloudtrail:GetEventSelectors",
            "iam:ListAttached*",
            "ec2:Request*",
            "sqs:*",
            "iam:PassRole",
            "ses:*",
            "ec2:Import*",
            "ec2:Release*",
            "iam:GetRole*",
            "ec2:Purchase*",
            "ec2:Bundle*",
            "elasticfilesystem:*",
            "s3:*",
            "ec2:Copy*",
            "ec2:Replace*",
            "sts:*",
            "iam:ListRoles",
            "elasticloadbalancing:*",
            "iam:Simulate*",
            "ec2:Describe*",
            "cloudtrail:ListPublicKeys",
            "iam:GetContextKeys*",
            "route53:*",
            "ec2:Allocate*",
            "ecr:*",
            "iam:Upload*",
            "ssm:*",
            "lambda:*",
            "glue:*",
            "firehose:*",
            "athena:*",
            "application-autoscaling:*",
            "scheduler:*",
            "transfer:*"
          ],
          "Effect" : "Allow",
          "Resource" : "*",
          "Sid" : "VisualEditor2"
        },
        {
          "Action" : [
            "kms:CreateGrant",
            "kms:Decrypt",
            "kms:DescribeKey",
            "kms:Encrypt*",
            "kms:GenerateDataKey",
            "kms:GenerateDataKeyWithoutPlaintext",
            "kms:ReEncrypt*",
            "kms:GetKeyPolicy",
            "kms:GetKeyRotationStatus",
            "kms:ListResourceTags",
          ]
          "Effect" : "Allow",
          "Resource" : concat(
            [
              "${local.bfd_insights_kms_key_id}",
              "${local.kms_key_id}",
              "${local.tf_state_kms_key_id}",
              "${local.test_kms_key_id}",
              "${local.prod_sbx_kms_key_id}",
              "${local.prod_kms_key_id}"
            ],
            local.all_kms_config_key_arns
          ),
          "Sid" : "AllowRoutineKeyAccess"
        },
        {
          "Action" : [
            "kms:ListAliases"
          ],
          "Effect" : "Allow",
          "Resource" : "*",
          "Sid" : "AllowAliasListingKms"
        }
      ],
      "Version" : "2012-10-17"
  })
}

resource "aws_iam_user" "jenkins_user" {
  force_destroy = false
  name          = "bfd-${local.env}-jenkins"
  path          = "/"
}

resource "aws_iam_access_key" "jenkins_user_key" {
  user = aws_iam_user.jenkins_user.name
}

resource "aws_ssm_parameter" "jenkins_user_key_secret" {
  key_id    = data.aws_kms_key.config_cmk.arn
  name      = "/bfd/mgmt/common/sensitive/user/bfd-mgmt-jenkins/aws_secret_key"
  overwrite = true
  type      = "SecureString"
  value     = nonsensitive(aws_iam_access_key.jenkins_user_key.secret)
}

resource "aws_ssm_parameter" "jenkins_user_key_id" {
  key_id    = data.aws_kms_key.config_cmk.arn
  name      = "/bfd/mgmt/common/sensitive/user/bfd-mgmt-jenkins/aws_access_id"
  overwrite = true
  type      = "SecureString"
  value     = aws_iam_access_key.jenkins_user_key.id
}

resource "aws_iam_group" "jenkins_user_group" {
  name = "bfd-${local.env}-jenkins"
  path = "/"
}

resource "aws_iam_group_membership" "jenkins_user_group_membership" {
  group = aws_iam_group.jenkins_user_group.name
  name  = "bfd-${local.env}-jenkins"
  users = [aws_iam_user.jenkins_user.name]
}

resource "aws_iam_group_policy_attachment" "jenkins_user_group_attachment" {
  group      = aws_iam_group.jenkins_user_group.id
  policy_arn = aws_iam_policy.jenkins_permission_boundary.arn
}
