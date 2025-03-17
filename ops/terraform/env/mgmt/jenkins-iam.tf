data "aws_kms_alias" "sse_s3" {
  name = "alias/aws/s3"
}

# TODO: Further separate the concerns and provide targeted permissions for Jenkins
resource "aws_iam_policy" "jenkins_permission_boundary" {
  description = "Jenkins Permission Boundary Policy"
  name        = "bfd-${local.env}-jenkins-permission-boundary"
  path        = local.cloudtamer_iam_path
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
          "Sid" : "AllowDescribeDefaultS3BucketKey",
          "Effect" : "Allow",
          "Action" : [
            "kms:DescribeKey"
          ],
          "Resource" : data.aws_kms_alias.sse_s3.target_key_arn
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

