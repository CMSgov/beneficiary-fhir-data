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
              "${local.bfd_insights_kms_key_id}",
              "${local.kms_key_id}",
              "${local.tf_state_kms_key_id}",
              "${local.test_kms_key_id}",
              "${local.prod_sbx_kms_key_id}",
              "${local.prod_kms_key_id}"
            ],
            local.all_kms_config_key_arns
          )
        }
      ],
      "Version" : "2012-10-17"
    }
  )
}

resource "aws_iam_policy" "code_artifact_rw" {
  description = "CodeArtifact read/write permissions"
  name        = "bfd-${local.env}-codeartifact-rw"
  path        = "/"
  policy      = <<-POLICY
{
  "Statement": [
    {
      "Action": [
        "codeartifact:CopyPackageVersions",
        "codeartifact:DeletePackageVersions",
        "codeartifact:DescribePackageVersion",
        "codeartifact:DescribeRepository",
        "codeartifact:GetPackageVersionAsset",
        "codeartifact:GetPackageVersionReadme",
        "codeartifact:GetRepositoryEndpoint",
        "codeartifact:ListPackageVersionAssets",
        "codeartifact:ListPackageVersionDependencies",
        "codeartifact:ListPackageVersions",
        "codeartifact:ListPackages",
        "codeartifact:PublishPackageVersion",
        "codeartifact:PutPackageMetadata",
        "codeartifact:ReadFromRepository",
        "codeartifact:TagResource",
        "codeartifact:UntagResource",
        "codeartifact:UpdatePackageVersionsStatus",
        "codeartifact:GetAuthorizationToken"
      ],
      "Effect": "Allow",
      "Resource": [
        "arn:aws:codeartifact:*:${local.account_id}:package/*/*/*/*/*",
        "${aws_codeartifact_repository.this.arn}",
        "${aws_codeartifact_domain.this.arn}"
      ],
      "Sid": "CodeArtifactReadWrite"
    },
    {
      "Action": "sts:GetServiceBearerToken",
      "Effect": "Allow",
      "Resource": "*",
      "Sid": "TempCreds"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
}

resource "aws_iam_policy" "code_artifact_ro" {
  description = "Allows Access to env:mgmt AWS Code Artifact Resources"
  name        = "bfd-${local.env}-code-artifact-ro"
  path        = "/"
  policy      = <<-POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "CodeArtifactDownload",
            "Effect": "Allow",
            "Action": [
                "codeartifact:GetPackageVersionReadme",
                "codeartifact:GetAuthorizationToken",
                "codeartifact:ListPackages",
                "codeartifact:ReadFromRepository",
                "codeartifact:GetRepositoryEndpoint",
                "codeartifact:DescribePackageVersion",
                "codeartifact:GetPackageVersionAsset",
                "codeartifact:ListPackageVersions"
            ],
            "Resource": [
                "arn:aws:codeartifact:us-east-1:${local.account_id}:package/*/*/*/*/*",
                "${aws_codeartifact_repository.this.arn}",
                "${aws_codeartifact_domain.this.arn}"
            ]
        },
        {
            "Sid": "TempCreds",
            "Effect": "Allow",
            "Action": "sts:GetServiceBearerToken",
            "Resource": "*"
        }
    ]
}
POLICY
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
    aws_iam_policy.jenkins_volume.arn,
    aws_iam_policy.code_artifact_rw.arn,
  ]
  max_session_duration = 3600
  name                 = "cloudbees-jenkins"
  path                 = "/"

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
  policy = jsonencode(
    {
      "Statement" : [
        {
          "Sid" : "ListParameters",
          "Action" : [
            "ssm:DescribeParameters"
          ],
          "Effect" : "Allow",
          "Resource" : [
            "arn:aws:ssm:us-east-1:${local.account_id}:*"
          ]
        },
        {
          "Sid" : "AllowReadBFDParams",
          "Action" : [
            "ssm:GetParametersByPath",
            "ssm:GetParameters",
            "ssm:GetParameter"
          ],
          "Effect" : "Allow",
          "Resource" : [
            "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/*"
          ]
        },
        {
          "Sid" : "AllowKeyUsage",
          "Action" : [
            "kms:Decrypt"
          ],
          "Effect" : "Allow",
          "Resource" : local.all_kms_config_key_arns
        }
      ],
      "Version" : "2012-10-17"
    }
  )
}

resource "aws_iam_group_policy_attachment" "app_engineers_bfd_ssm_ro" {
  group      = aws_iam_group.app_engineers.id
  policy_arn = aws_iam_policy.bfd_ssm_ro.arn
}

resource "aws_iam_policy" "rda_ec2_instance_manager" {
  name        = "bfd-rda-pipeline-ec2-instance-manager"
  description = "Allow management of RDA pipeline instances"
  path        = "/"
  policy      = <<-POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "RDAPipelineInstanceAdmin",
            "Effect": "Allow",
            "Action": [
                "ec2:RebootInstances",
                "ec2:TerminateInstances",
                "ec2:StartInstances",
                "ec2:StopInstances"
            ],
            "Resource": [
                "arn:aws:ec2:us-east-1:${local.account_id}:instance/*-pipeline-rda"
            ]
        }
    ]
}
POLICY
}

resource "aws_iam_policy" "rda_ssm_ro" {
  description = "Allow reading RDA pipeline SSM hierarchies"
  name        = "bfd-rda-pipeline-ssm-parameters-ro"
  path        = "/"
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Statement" : [
        {
          "Sid" : "ReadRDAPipelineSSMParameters",
          "Action" : [
            "ssm:GetParametersByPath",
            "ssm:GetParameters",
            "ssm:GetParameter"
          ],
          "Effect" : "Allow",
          "Resource" : [
            "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/*/pipeline/rda/*"
          ]
        },
        {
          "Sid" : "AllowKeyUsage",
          "Action" : [
            "kms:Decrypt"
          ],
          "Effect" : "Allow",
          "Resource" : local.all_kms_config_key_arns
        }
      ]
    }
  )
}
