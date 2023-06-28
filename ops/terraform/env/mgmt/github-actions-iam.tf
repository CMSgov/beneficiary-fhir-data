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
        "s3:ListBucket",
        "s3:GetBucketLocation"
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
