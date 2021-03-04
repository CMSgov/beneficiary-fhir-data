resource "aws_kms_key" "master_key" {
  description = "bfd-${var.env_config.env}-master-key"
  tags        = var.env_config.tags

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "bfd-${var.env_config.env}-master-key-policy",
  "Statement": [
    {
      "Sid": "Admin Permissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "arn:aws:iam::577373831711:user/VZG9",
          "arn:aws:iam::577373831711:user/ECZK"
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
        "AWS": "arn:aws:iam::577373831711:role/bfd-${var.env_config.env}-app-role"
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
