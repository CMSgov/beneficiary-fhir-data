##
# Provisions an Elastic Container Registry

locals {
  tags = merge({ Name = var.name }, var.env_config.tags)
}

# current account number
data "aws_caller_identity" "current" {}


## Define IAM Access Policies
#

# read only access
data "aws_iam_policy_document" "read_only" {
  statement {
    sid    = "ReadonlyAccess"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:DescribeImageScanFindings",
      "ecr:DescribeImages",
      "ecr:DescribeRepositories",
      "ecr:GetDownloadUrlForLayer",
      "ecr:GetLifecyclePolicy",
      "ecr:GetLifecyclePolicyPreview",
      "ecr:GetRepositoryPolicy",
      "ecr:ListImages",
      "ecr:ListTagsForResource",
    ]
    resources = [aws_ecr_repository.main.arn]
  }
}

# full access
data "aws_iam_policy_document" "full_access" {
  statement {
    sid       = "FullAccess"
    effect    = "Allow"
    actions   = ["ecr:*"]
    resources = [aws_ecr_repository.main.arn]
  }
}


## IAM Groups
# note: group membership is managed out-of-band

# create a read only group (add members using the console)
resource "aws_iam_group" "read_only" {
  name = "${aws_ecr_repository.main.name}-read-only-access"
}

# create a full access group (add members using the console)
resource "aws_iam_group" "full_access" {
  name = "${aws_ecr_repository.main.name}-full-access"
}

# read only group policy
resource "aws_iam_group_policy" "ecr_read_only" {
  group  = aws_iam_group.read_only.name
  policy = data.aws_iam_policy_document.read_only.json
}

# full access group policy
resource "aws_iam_group_policy" "full_access" {
  group  = aws_iam_group.read_only.name
  policy = data.aws_iam_policy_document.full_access.json
}


## ECR repo
#

resource "aws_ecr_repository" "main" {
  name = var.name

  image_tag_mutability = var.image_tag_mutability
  image_scanning_configuration {
    scan_on_push = var.scan_images_on_push
  }

  # if an encryption config is present, use it and encrypt the repo
  dynamic "encryption_configuration" {
    for_each = var.encryption_configuration == null ? [] : [var.encryption_configuration]
    content {
      encryption_type = encryption_configuration.value["encryption_type"]
      kms_key         = encryption_configuration.value["kms_key"]
    }
  }
}

## Repo lifecycle policy
resource "aws_ecr_lifecycle_policy" "lifecycle" {
  repository = aws_ecr_repository.main.name

  policy = var.lifecycle_policy
}
