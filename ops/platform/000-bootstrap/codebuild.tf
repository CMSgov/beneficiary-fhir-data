locals {
  codebuild_runner_config = {
    # Default Runner suited for low-cost, fast-starting Runner jobs that don't exceed 15 minutes
    lambda = {
      name         = "bfd-${local.account_type}-platform-lambda"
      compute_type = "BUILD_LAMBDA_2GB"
      type         = "ARM_LAMBDA_CONTAINER"
      image        = "${aws_ecr_repository.codebuild_runner.repository_url}:${local.bfd_version}"
      privileged   = false
      timeout      = 15
    }
    # Runner for long-running (>= 15 minutes) Terraservice applies or other jobs
    small = {
      name         = "bfd-${local.account_type}-platform-small"
      compute_type = "BUILD_GENERAL1_SMALL"
      type         = "ARM_CONTAINER"
      image        = "${aws_ecr_repository.codebuild_runner.repository_url}:${local.bfd_version}"
      privileged   = false
      timeout      = 60
    }
    large = {
      name         = "bfd-${local.account_type}-platform-large"
      compute_type = "BUILD_GENERAL1_LARGE"
      type         = "ARM_CONTAINER"
      image        = "${aws_ecr_repository.codebuild_runner.repository_url}:${local.bfd_version}"
      privileged   = false
      timeout      = 60
    }
    # Runner for building Docker images and nothing else
    docker = {
      name         = "bfd-${local.account_type}-platform-docker"
      compute_type = "BUILD_GENERAL1_SMALL"
      type         = "ARM_CONTAINER"
      image        = "aws/codebuild/amazonlinux-aarch64-standard:3.0"
      privileged   = true # Enables this Runner to build Docker images
      timeout      = 60
    }
  }
}

# ECR resources are defined in this Terraservice opposed to "ecr" as the CodeBuild Runner image
# needs to exist in order for the CodeBuild Runners to work at all. So, this repository must exist
# earlier than the "ecr" Terraservice
resource "aws_ecr_repository" "codebuild_runner" {
  name                 = "bfd-platform-codebuild-runner"
  image_tag_mutability = "IMMUTABLE"

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = aws_kms_key.primary["platform"].arn
  }

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "codebuild_runner" {
  repository = aws_ecr_repository.codebuild_runner.name

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

# This resource is *special* and _requires_ that the operator applying this IaC use the console to
# confirm the connection. There is no AWS API or CLI command that is able to confirm the connection
# automatically. This is a limitation of CodeBuild itself.
# As of 07/25, this connection is currently attached to a GitHub account and not the "AWS Connector"
# application.
# TODO: Use the AWS Connector app for this CodeStar Connection
resource "aws_codestarconnections_connection" "github" {
  name          = "bfd-platform-github-connection"
  provider_type = "GitHub"
}

resource "aws_codebuild_source_credential" "github" {
  auth_type   = "CODECONNECTIONS"
  server_type = "GITHUB"
  token       = aws_codestarconnections_connection.github.arn
}

resource "aws_cloudwatch_log_group" "runner" {
  for_each = local.codebuild_runner_config

  name       = "/aws/codebuild/${each.value.name}"
  kms_key_id = aws_kms_key.primary["platform"].arn
}

resource "aws_codebuild_project" "runner" {
  depends_on = [aws_codebuild_source_credential.github]
  for_each   = local.codebuild_runner_config

  name               = each.value.name
  build_timeout      = each.value.timeout
  project_visibility = "PRIVATE"
  service_role       = aws_iam_role.codebuild[each.key].arn

  artifacts {
    type = "NO_ARTIFACTS"
  }

  environment {
    compute_type                = each.value.compute_type
    image                       = each.value.image
    image_pull_credentials_type = startswith(each.value.image, "aws/codebuild") ? "CODEBUILD" : "SERVICE_ROLE"
    privileged_mode             = each.value.privileged
    type                        = each.value.type
  }

  logs_config {
    cloudwatch_logs {
      status     = "ENABLED"
      group_name = aws_cloudwatch_log_group.runner[each.key].name
    }
  }

  source {
    insecure_ssl        = false
    location            = "https://github.com/CMSgov/beneficiary-fhir-data"
    report_build_status = false
    type                = "GITHUB"
  }
}

resource "aws_codebuild_webhook" "runner" {
  for_each = local.codebuild_runner_config

  project_name = aws_codebuild_project.runner[each.key].name
  build_type   = "BUILD"

  filter_group {
    filter {
      exclude_matched_pattern = false
      pattern                 = "WORKFLOW_JOB_QUEUED"
      type                    = "EVENT"
    }
  }
}
