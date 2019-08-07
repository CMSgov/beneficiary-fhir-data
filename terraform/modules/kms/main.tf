provider "aws" {
  region = "us-east-1"
}

resource "aws_kms_key" "app-config-key" {
  description = "bfd-${var.env}-app-config"

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "bfd-${var.env}-app-config-key-policy",
  "Statement": [
    {
      "Sid": "Admin Permissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "arn:aws:iam::577373831711:user/VZG9"
        ]
      },
      "Action": [
          "kms:Create*",
          "kms:Describe*",
          "kms:Enable*",
          "kms:List*",
          "kms:Put*",
          "kms:Update*",
          "kms:Revoke*",
          "kms:Disable*",
          "kms:Get*",
          "kms:Delete*",
          "kms:ScheduleKeyDeletion",
          "kms:CancelKeyDeletion"
      ],
      "Resource": "*"
    },
{
      "Sid": "User Permissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "arn:aws:iam::577373831711:user/VZG9"
        ]
      },
      "Action": [
        "kms:Encrypt",
        "kms:Decrypt",
        "kms:ReEncrypt",
        "kms:GenerateDataKey*",
        "kms:DescribeKey"
      ],
      "Resource": "*"
    },
    {
      "Sid": "Allow instance role to decrypt",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::577373831711:role/bfd-${var.env}-app-role"
      },
      "Action": "kms:Decrypt",
      "Resource": "*"
    }
  ]
}
POLICY
}

resource "aws_kms_alias" "app-config-key-alias" {
  name          = "alias/bfd-${var.env}-app-config"
  target_key_id = "${aws_kms_key.app-config-key.key_id}"
}
