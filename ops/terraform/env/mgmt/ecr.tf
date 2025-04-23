locals {
  ecr_container_repositories = toset([
    # utility container image repositories
    "bfd-mgmt-eft-sftp-outbound-transfer-lambda",
    "bfd-mgmt-pipeline-ccw-manifests-verifier-lambda",
    "bfd-mgmt-server-load-broker",
    "bfd-mgmt-server-load-controller",
    "bfd-mgmt-server-load-node",
    "bfd-mgmt-server-regression",
    "bfd-mgmt-run-locust",
    "bfd-mgmt-pipeline-ccw-runner",
    "bfd-mgmt-synthea-generation",
    # base container image repositories
    "bfd-mgmt-base-python",
    "bfd-mgmt-base-java",
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

resource "aws_ecr_repository" "this" {
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

resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = local.ecr_container_repositories
  repository = each.value
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire images older than 90 days"
        selection = {
          tagStatus      = "tagged"
          countType      = "sinceImagePushed"
          countUnit      = "days"
          countNumber    = 90
          tagPatternList = ["*"]
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Ensure at least one image is retained"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 1
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
