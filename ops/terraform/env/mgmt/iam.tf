resource "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  description = "ansible vault pw read only S3 policy"
  name        = "bfd-${local.env}-ansible-vault-pw-ro-s3"
  path        = "/"
  policy      = <<-POLICY
{
  "Statement": [
    {
      "Action": [
        "kms:Decrypt",
        "s3:GetObject"
      ],
      "Effect": "Allow",
      "Resource": [
        "${data.aws_kms_key.cmk.arn}",
        "${aws_s3_bucket.admin.arn}/ansible/vault.password"
      ],
      "Sid": "AnsibleVaultPwRO"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
  tags        = local.shared_tags
}

resource "aws_iam_policy" "github_actions_s3its" {
  description = "GitHub Actions policy for S3 integration tests"
  name        = "bfd-${local.env}-github-actions-s3its"
  path        = "/"
  policy      = <<-POLICY
{
  "Statement": [
    {
      "Action": [
        "s3:CreateBucket",
        "s3:ListAllMyBuckets"
      ],
      "Effect": "Allow",
      "Resource": "*",
      "Sid": "BFDGitHubActionsS3ITs"
    },
    {
      "Action": [
        "s3:DeleteBucket",
        "s3:HeadBucket",
        "s3:ListBucket"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::bb-test-*",
      "Sid": "BFDGitHubActionsS3ITsBucket"
    },
    {
      "Action": [
        "s3:DeleteObject",
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::bb-test-*/*",
      "Sid": "BFDGitHubActionsS3ITsObject"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
  tags        = local.shared_tags
}

#TODO: Determine if the bfd-packages sees continued use
resource "aws_iam_policy" "packer_s3" {
  description = "packer S3 Policy"
  name        = "bfd-${local.env}-packer-s3"
  path        = "/"
  policy      = <<-POLICY
{
  "Statement": [
    {
      "Action": [
        "s3:GetObjectAcl",
        "s3:GetObject",
        "s3:GetObjectVersionAcl",
        "s3:GetObjectTagging",
        "s3:ListBucket",
        "s3:GetObjectVersion"
      ],
      "Effect": "Allow",
      "Resource": [
        "arn:aws:s3:::bfd-packages/*",
        "arn:aws:s3:::bfd-packages"
      ],
      "Sid": "BFDProfile"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
  tags        = local.shared_tags
}

resource "aws_iam_user" "github_actions" {
  force_destroy = false
  name          = "bfd-${local.env}-github-actions"
  path          = "/"
  tags          = local.shared_tags
}

resource "aws_iam_policy" "jenkins_permission_boundary" {
  description = "Jenkins Permission Boundary Policy"
  name        = "bfd-${local.env}-jenkins-permission-boundary"
  path        = "/"
  policy      = <<-POLICY
{
  "Statement": [
    {
      "Action": [
        "kms:List*",
        "rds:*",
        "ec2:Reset*",
        "cloudtrail:GetTrailStatus",
        "logs:*",
        "kms:Get*",
        "dynamodb:*",
        "autoscaling:*",
        "kms:ReEncrypt*",
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
        "kms:Describe*",
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
        "kms:*",
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
        "glue:*"
      ],
      "Effect": "Allow",
      "Resource": "*",
      "Sid": "VisualEditor2"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
  tags        = local.shared_tags
}

resource "aws_iam_policy" "jenkins_volume" {
  description = "Jenkins Data Volume Policy"
  name        = "bfd-${local.env}-jenkins-volume"
  path        = "/"
  policy      = <<-POLICY
{
  "Statement": [
    {
      "Action": "ec2:AttachVolume",
      "Effect": "Allow",
      "Resource": [
        "arn:aws:ec2:*:*:instance/*",
        "arn:aws:ec2:*:*:volume/*"
      ]
    },
    {
      "Action": "ec2:DescribeVolumes",
      "Effect": "Allow",
      "Resource": "*"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
  tags        = local.shared_tags
}

resource "aws_iam_role" "cloudbees" {
  assume_role_policy    = <<-POLICY
{
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Condition": {},
      "Effect": "Allow",
      "Principal": {
        "AWS": "${data.aws_ssm_parameter.cbc_aws_account_arn.value}"
      }
    }
  ],
  "Version": "2012-10-17"
}
POLICY
  description           = "Assume role for our cloudbees jenkins"
  force_detach_policies = false
  managed_policy_arns = [
    aws_iam_policy.jenkins_permission_boundary.arn,
    aws_iam_policy.jenkins_volume.arn
  ]
  max_session_duration = 3600
  name                 = "cloudbees-jenkins"
  path                 = "/"
  tags                 = local.shared_tags
}

resource "aws_iam_group" "app_engineers" {
  name = "bfd-app-engineers"
  path = "/"
}

resource "aws_iam_group_policy" "ec2_instance_manager" {
  group  = aws_iam_group.app_engineers.id
  name   = "ec2-instance-manager"
  policy = <<-POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "ec2:RebootInstances",
                "ec2:TerminateInstances",
                "ec2:StartInstances",
                "ec2:StopInstances"
            ],
            "Resource": [
                "arn:aws:ec2:us-east-1:${local.account_id}:instance/*",
                "arn:aws:license-manager:*:${local.account_id}:license-configuration/*"
            ]
        }
    ]
}
POLICY
}

resource "aws_iam_policy" "s3_integration_tests" {
  name   = "bfd-s3-for-integration-tests"
  path   = "/"
  policy = <<-POLICY
{
  "Statement": [
    {
      "Action": [
        "s3:PutEncryptionConfiguration",
        "s3:PutBucketNotification",
        "s3:ListBucketMultipartUploads",
        "s3:PutBucketTagging",
        "s3:PutBucketLogging",
        "s3:GetBucketLogging",
        "s3:ListBucketVersions",
        "s3:CreateBucket",
        "s3:ListBucket",
        "s3:DeleteBucket",
        "s3:PutBucketVersioning"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::bb-test-*",
      "Sid": "AllowCreateDelete"
    },
    {
      "Action": [
        "s3:DeleteObjectTagging",
        "s3:DeleteObjectVersion",
        "s3:GetObjectVersionTagging",
        "s3:RestoreObject",
        "s3:PutObjectVersionTagging",
        "s3:DeleteObjectVersionTagging",
        "s3:PutObject",
        "s3:GetObject",
        "s3:AbortMultipartUpload",
        "s3:GetObjectTagging",
        "s3:PutObjectTagging",
        "s3:DeleteObject",
        "s3:GetObjectVersion"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::bb-test-*/*",
      "Sid": "AllowReadAndWrite"
    },
    {
      "Action": "s3:ListAllMyBuckets",
      "Effect": "Allow",
      "Resource": "*",
      "Sid": "AllowListingBuckets"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
  tags   = local.shared_tags
}

resource "aws_iam_group_policy_attachment" "app_engineers_s3_integration_tests" {
  group      = aws_iam_group.app_engineers.id
  policy_arn = aws_iam_policy.s3_integration_tests.arn
}

resource "aws_iam_group_policy_attachment" "app_engineers_autoscaling_ro" {
  group      = aws_iam_group.app_engineers.id
  policy_arn = "arn:aws:iam::aws:policy/AutoScalingReadOnlyAccess"
}

resource "aws_iam_group_policy_attachment" "app_engineers_ec2_ro" {
  group      = aws_iam_group.app_engineers.id
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ReadOnlyAccess"
}

resource "aws_iam_group_policy_attachment" "app_engineers_vpc_ro" {
  group      = aws_iam_group.app_engineers.id
  policy_arn = "arn:aws:iam::aws:policy/AmazonVPCReadOnlyAccess"
}

resource "aws_iam_policy" "bfd_ssm_ro" {
  description = "Permissions to /bfd SSM hierarchies"
  name        = "bfd-ssm-parameters-ro"
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
        "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/*"
      ]
    },
    {
      "Action": [
        "kms:Decrypt"
      ],
      "Effect": "Allow",
      "Resource": [
        "${local.kms_key_id}",
        "${local.test_kms_key_id}",
        "${local.prod_sbx_kms_key_id}",
        "${local.prod_kms_key_id}"
      ]
    }
  ],
  "Version": "2012-10-17"
}
POLICY
  tags        = local.shared_tags
}

resource "aws_iam_group_policy_attachment" "app_engineers_bfd_ssm_ro" {
  group      = aws_iam_group.app_engineers.id
  policy_arn = aws_iam_policy.bfd_ssm_ro.arn
}
