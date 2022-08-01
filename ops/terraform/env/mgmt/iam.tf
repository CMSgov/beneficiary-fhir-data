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
