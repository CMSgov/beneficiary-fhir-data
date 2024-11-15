resource "aws_iam_policy" "packer_s3" {
  #FIXME: This policy should be reconsidered before acceptance of BFD-3698. Suggest removal.
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
}

resource "aws_iam_policy" "packer_ssm" {
  description = "Policy granting permission for bfd-packer profiled instances to access some common SSM hierarchies"
  name        = "bfd-${local.env}-packer-ssm"
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
        %{for env in local.established_envs~}
        "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${env}/common/*",
        %{endfor~}
        "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/*"
      ],
      "Sid": "BFDProfile"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
}

resource "aws_iam_policy" "packer_kms" {
  description = "Policy granting permission for bfd-packer profiled instances to decrypt using mgmt and established environment KMS keys"
  name        = "bfd-${local.env}-packer-kms"
  path        = "/"
  policy = jsonencode(
    {
      "Statement" : [
        {
          "Action" : ["kms:Decrypt"],
          "Effect" : "Allow",
          "Resource" : concat(
            [
              local.bfd_insights_kms_key_id,
              local.kms_key_id,
              local.tf_state_kms_key_id,
              local.test_kms_key_id,
              local.prod_sbx_kms_key_id,
              local.prod_kms_key_id
            ],
            local.all_kms_config_key_arns
          )
        }
      ],
      "Version" : "2012-10-17"
    }
  )
}

resource "aws_iam_role" "packer" {
  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "ec2.amazonaws.com"
          }
        },
      ]
      Version = "2012-10-17"
    }
  )
  description           = "Allows EC2 instances to call AWS services on your behalf."
  force_detach_policies = false
  managed_policy_arns = [
    aws_iam_policy.packer_ssm.arn,
    aws_iam_policy.packer_s3.arn,
    aws_iam_policy.packer_kms.arn,
    "arn:aws:iam::aws:policy/AmazonElasticFileSystemFullAccess", #FIXME: This should be reconsidered before acceptance of BFD-3698. Suggest removal.
    aws_iam_policy.ec2_instances_tags_ro.arn,
  ]
  max_session_duration = 3600
  name                 = "bfd-packer"
  path                 = "/"
}

resource "aws_iam_instance_profile" "packer" {
  name = aws_iam_role.packer.name
  role = aws_iam_role.packer.name
  path = "/"
}
