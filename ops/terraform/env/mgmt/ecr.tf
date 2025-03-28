locals {
  #Defined Admin Roles with Access to perform destructive actions on the repository
  admin_roles_arn = toset([
    aws_iam_role.github_actions.arn
  ])

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
#Enforce a general policy for all repository in the registry
resource "aws_ecr_registry_policy" "for_bfd" {

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      #Deny request if the Principal is not an Assumed Role with an IAM role in the account
      {
        Effect = "Deny",
        Principal = {
          "AWS": "*"
        },
        Action = ["ecr:*"],
        Condition = {
          "StringNotLike" = {
            "aws:PrincipalType" = "AssumedRole"
          },
          "StringNotEquals": {
            "aws:SourceAccount": "${local.account_id}"
          }
        }
      },

      #Deny destructive actions on the repository if user does not have the administrative role to perform the action
      {
        Effect = "Deny",
        Principal = {
          "AWS": "*"
        },
        Action = [
          "ecr:BatchDeleteImage",
          "ecr:DeleteRepository",
          "ecr:DeleteRepositoryPolicy",
          "ecr:PutImageTagMutability",
          "ecr:DeleteLifecyclePolicy"
        ],
        Condition = {
          "ArnNotEquals" = {
            "aws:PrincipalArn" = local.admin_roles_arn 
          }
        }
      },

      #untagged images are not allow
      {
        Effect = "Deny",
        Action = "ecr:PutImage",
        Condition = {
          "StringLike" = {
            "ecr:ImageTag": [""]
          }
        }
      }
    ]
  })
}

#BFD-3965
resource "aws_ecr_lifecycle_policy" "for_bfd_repositories" {
    for_each = local.ecr_container_repositories
    repository = each.value
    policy = jsonencode({
      rules = [
        {
          rulePriority = 1
          description = "Expire images older than 90 days"
          selection = {
            tagStatus = "tagged"
            countType = "sinceImagePushed"
            countUnit = "days"
            countNumber = 90
          }
          action = {
            type = "expire"
          }
        },
        {
          rulePriority = 2
          description = "Ensure at least one image is retained"
          selection = {
            tagStatus = "any"
            countType = "imageCountMoreThan"
            countNumber = 1
          }
          action = {
            type = "expire"
          }
        }
      ]
    })
}
