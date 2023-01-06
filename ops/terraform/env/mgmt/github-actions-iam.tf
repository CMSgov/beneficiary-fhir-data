# TODO: Consider using an OIDC authentication workflow instead of a service-specific IAM User
#       This requires better solutions before the end of 2022 to avoid AWS IAM key rotation maintenance
resource "aws_iam_group" "github_actions" {
  name = "bfd-mgmt-github-actions"
  path = "/"
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
    },
    {
        "Sid": "BFDGitHubActionsS3ITsBucketSecurity",
        "Effect": "Allow",
        "Action": [
            "s3:PutEncryptionConfiguration",
            "s3:PutBucketPublicAccessBlock",
            "s3:PutBucketAcl",
            "s3:PutBucketPolicy"
        ],
        "Resource": "arn:aws:s3:::bb-test-*"
    }
  ],
  "Version": "2012-10-17"
}
POLICY

}

resource "aws_iam_group_policy_attachment" "github_actions_1" {
  group      = aws_iam_group.github_actions.id
  policy_arn = aws_iam_policy.github_actions_s3its.arn
}

resource "aws_iam_group_policy_attachment" "github_actions_2" {
  group      = aws_iam_group.github_actions.id
  policy_arn = aws_iam_policy.code_artifact_ro.arn
}

resource "aws_iam_user" "github_actions" {
  force_destroy = false
  name          = "bfd-${local.env}-github-actions"
  path          = "/"
}

resource "aws_iam_access_key" "github_actions" {
  user = aws_iam_user.github_actions.name
}

resource "aws_ssm_parameter" "github_actions" {
  key_id    = local.kms_key_id
  name      = "/bfd/mgmt/github/sensitive/iam_access_key"
  overwrite = true
  type      = "SecureString"
  value = jsonencode({
    "aws_access_key_id"     = aws_iam_access_key.github_actions.id
    "aws_secret_access_key" = aws_iam_access_key.github_actions.secret
  })
}

resource "aws_iam_group_membership" "github_actions" {
  name = "github-actions"

  users = [
    aws_iam_user.github_actions.name
  ]

  group = aws_iam_group.github_actions.name
}

data "tls_certificate" "github_actions" {
  url = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github_actions.certificates[0].sha1_fingerprint]
  url             = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_role" "github_actions" {
  name        = "bfd-${local.env}-github-actions"
  path        = "/"
  description = "OIDC Assumable GitHub Actions Role"

  managed_policy_arns = [
    aws_iam_policy.code_artifact_ro.arn,
    aws_iam_policy.github_actions_s3its.arn
  ]

  assume_role_policy = jsonencode(
    {
      "Statement" : [
        {
          "Action" : "sts:AssumeRoleWithWebIdentity",
          "Condition" : {
            "ForAllValues:StringLike" : {
              "token.actions.githubusercontent.com:sub" : "repo:CMSgov/beneficiary-fhir-data:*",
              "token.actions.githubusercontent.com:aud" : "sts.amazonaws.com"
            }
          },
          "Effect" : "Allow",
          "Principal" : {
            "Federated" : aws_iam_openid_connect_provider.github_actions.arn
          }
        }
      ],
      "Version" : "2012-10-17"
  })
}
