terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  service              = local.service
  relative_module_root = "ops/platform/02-ecr"
}

locals {
  service = "ecr"

  region       = module.terraservice.region
  account_id   = module.terraservice.account_id
  default_tags = module.terraservice.default_tags
  kms_key_arn  = module.terraservice.key_arn

  ecr_container_repositories = toset([
    # utility container image repositories
    "bfd-platform-eft-sftp-outbound-transfer-lambda",
    "bfd-platform-pipeline-ccw-manifests-verifier-lambda",
    "bfd-platform-server-load-broker",
    "bfd-platform-server-load-controller",
    "bfd-platform-server-load-node",
    "bfd-platform-server-regression",
    "bfd-platform-run-locust",
    "bfd-platform-pipeline-ccw-runner",
    "bfd-platform-synthea-generation",
    # base container image repositories
    "bfd-platform-base-python",
    "bfd-platform-base-java",
    "bfd-platform-pipeline-idr",
    # sidecar container image repositories
    "bfd-platform-mount-certstores",
    "bfd-platform-server-fluent-bit",
    # application container image repositories
    "bfd-db-migrator",
    "bfd-pipeline-app",
    "bfd-server",
    "bfd-server-ng"
  ])
}

resource "aws_ecr_repository" "this" {
  for_each = local.ecr_container_repositories

  name                 = each.value
  image_tag_mutability = "IMMUTABLE"

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = local.kms_key_arn
  }

  image_scanning_configuration {
    scan_on_push = true
  }
}

data "aws_iam_policy_document" "this" {
  statement {
    sid = "AllowAWSLambdaECRImageRetrieval"
    actions = [
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer"
    ]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_ecr_repository_policy" "name" {
  for_each = local.ecr_container_repositories

  policy     = data.aws_iam_policy_document.this.json
  repository = aws_ecr_repository.this[each.key].name
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each = local.ecr_container_repositories

  repository = aws_ecr_repository.this[each.key].name

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
