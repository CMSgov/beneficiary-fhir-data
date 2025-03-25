locals {
  #ECR Pull/Push IAM Roles
  ecr_pull_role_name = "ecr_pull_role"
  ecr_push_role_name = "ecr_push_role"

  ecr_container_repositories = toset([
    # utility container image repositories
    "bfd-mgmt-eft-sftp-outbound-transfer-lambda",
    "bfd-mgmt-pipeline-ccw-manifests-verifier-lambda",
    "bfd-mgmt-server-load-broker",
    "bfd-mgmt-server-load-controller",
    "bfd-mgmt-server-load-node",
    "bfd-mgmt-server-regression",
    "bfd-mgmt-synthea-generation",
    # base container image repositories
    "bfd-mgmt-base-python",
    "bfd-mgmt-base-jdk",
    "bfd-mgmt-pipeline-idr",
    # sidecar container image repositories
    "bfd-mgmt-mount-certstores",
    "bfd-mgmt-server-fluent-bit",
    # application container image repositories
    "bfd-db-migrator",
    "bfd-pipeline-app",
    "bfd-server",
  ])
}

resource "aws_ecr_repository" "bfd" {
  for_each = local.ecr_container_repositories

  name                 = each.value
  image_tag_mutability = "IMMUTABLE"

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = local.kms_key_id
  }

  image_scanning_configuration {
    scan_on_push = true
  }
}

#BFD-3965
resource "aws_ecr_repository_policy" "bfd_repo_policy" {
  for_each = local.ecr_container_repositories

  repository = each.value
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      #Allow to Pull Images by the PullImages role
      {
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${local.account_id}:role/${local.ecr_pull_role_name}"
        }
        Action = [
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchCheckLayerAvailability"
        ]
      },
      #Allow to Push Images by the PushImages role
      {
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${local.account_id}:role/${local.ecr_push_role_name}"
        }
        Action = [
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload"
        ]
      }
    ]
  })
}

#BFD-3965
resource "aws_ecr_lifecycle_policy" "bfd" {
    for_each = local.ecr_container_repositories
    repository = each.value
    policy = jsonencode({
      rules = [
        {
          rulePriority = 1
          description = "Expire images older than 90 days"
          selection = {
            tagStatus = "untagged"
            countType = "sinceImagePushed"
            countUnit = "days"
            countNumber = 90
          }
          action = {
            type = "expire"
          }
        }
      ]
    })
}
