# used to get our current account number
data "aws_caller_identity" "current" {}

# TODO: specify resource
resource "aws_kms_key" "master_key" {
  description = "bfd-${var.env_config.env}-master-key"
  tags        = var.env_config.tags

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "bfd-${var.env_config.env}-master-key-policy",
  "Statement": [
    {
      "Sid": "Allow instance role to decrypt",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/bfd-${var.env_config.env}-app-role"
      },
      "Action": "kms:Decrypt",
      "Resource": "*"
    }
  ]
}
POLICY
}

resource "aws_kms_alias" "app-config-key-alias" {
  name          = "alias/bfd-${var.env_config.env}-master-key"
  target_key_id = aws_kms_key.master_key.key_id
}


# kms key admin policy
resource "aws_iam_policy" "kms-key-admin" {
    name        = "kms-key-admin"
    path        = "/"
    description = "KMS Key admin policy"
    policy      = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:DescribeCustomKeyStores",
        "kms:ListKeys",
        "kms:DeleteCustomKeyStore",
        "kms:GenerateRandom",
        "kms:UpdateCustomKeyStore",
        "kms:ListAliases",
        "kms:DisconnectCustomKeyStore",
        "kms:CreateKey",
        "kms:ConnectCustomKeyStore",
        "kms:CreateCustomKeyStore"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": "kms:*",
      "Resource": [
        "arn:aws:kms:*:${data.aws_caller_identity.current.account_id}:key/*",
        "arn:aws:kms:*:${data.aws_caller_identity.current.account_id}:alias/*"
      ]
    }
  ]
}
POLICY
}

# key admins group
resource "aws_iam_group" "kms-key-admins" {
    name = "kms-key-admins"
    path = "/"
}

# attach key admin policy to the kms-key-admins group
resource "aws_iam_policy_attachment" "kms-key-admin-policy-attachment" {
    name       = "kms-key-admin-policy-attachment"
    policy_arn = aws_iam_policy.kms-key-admin
    groups     = aws_iam_group.kms-key-admins
}
