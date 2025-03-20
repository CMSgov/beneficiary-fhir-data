resource "aws_iam_policy" "ec2_instance_tags_ro" {
  description = "Global EC2 Instances and Tags RO Policy"
  name        = "bfd-${local.env}-ec2-instance-tags-ro"
  path        = local.cloudtamer_iam_path
  policy      = <<-POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EC2InstanceTagsRO",
      "Action": [
        "ec2:DescribeTags",
        "ec2:DescribeInstances"
      ],
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
POLICY
}

resource "aws_iam_policy" "code_artifact_rw" {
  description = "CodeArtifact read/write permissions"
  name        = "bfd-${local.env}-codeartifact-rw"
  path        = local.cloudtamer_iam_path
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
  path        = local.cloudtamer_iam_path
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
  path        = local.cloudtamer_iam_path
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

# TODO: remove this and related in BFD-3953
# NOTE: `cloudbees` is not to be migrated as part of the greenfield migration
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
  path   = local.cloudtamer_iam_path
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
  path        = local.cloudtamer_iam_path
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
  path        = local.cloudtamer_iam_path
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
  path        = local.cloudtamer_iam_path
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
