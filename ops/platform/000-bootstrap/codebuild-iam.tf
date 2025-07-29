data "aws_iam_policy_document" "codebuild_logs" {
  for_each = local.codebuild_runner_config

  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.runner[each.key].arn}:*"]
  }
}

resource "aws_iam_policy" "codebuild_logs" {
  for_each = local.codebuild_runner_config

  name = "${each.value.name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${each.value.name} CodeBuild Runner to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.codebuild_logs[each.key].json
}

data "aws_iam_policy_document" "codebuild_github" {
  statement {
    sid = "AllowGithubConnection"
    actions = [
      "codestar-connections:GetConnectionToken",
      "codestar-connections:GetConnection",
      "codeconnections:GetConnectionToken",
      "codeconnections:GetConnection",
      "codeconnections:UseConnection"
    ]
    resources = [
      aws_codestarconnections_connection.github.arn,
      replace(aws_codestarconnections_connection.github.arn, "codestar-connections", "codeconnections")
    ]
  }
}

resource "aws_iam_policy" "codebuild_github" {
  for_each = local.codebuild_runner_config

  name = "${each.value.name}-gh-connection"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${each.value.name} CodeBuild Runner to use the GitHub Code ",
    "Connection"
  ])
  policy = data.aws_iam_policy_document.codebuild_github.json
}

data "aws_iam_policy_document" "codebuild_ecr" {
  statement {
    sid       = "AllowECRAuthorization"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "AllowGetRunnerImage"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage"
    ]
    resources = [aws_ecr_repository.codebuild_runner.arn]
  }
}

resource "aws_iam_policy" "codebuild_ecr" {
  for_each = local.codebuild_runner_config

  name = "${each.value.name}-ecr"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${each.value.name} CodeBuild Runner to auth to ECR and pull the ",
    "${aws_ecr_repository.codebuild_runner.name} Image"
  ])
  policy = data.aws_iam_policy_document.codebuild_ecr.json
}

data "aws_iam_policy_document" "codebuild_kms" {
  statement {
    sid = "AllowEncryptionAndDecryptionOfMasterKeys"
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
    ]
    resources = [aws_kms_key.primary["platform"].arn, aws_kms_key.secondary["platform"].arn]
  }
}

resource "aws_iam_policy" "codebuild_kms" {
  for_each = local.codebuild_runner_config

  name = "${each.value.name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${each.value.name} CodeBuild Runner to use the ",
    "${aws_kms_alias.primary["platform"].name} key"
  ])
  policy = data.aws_iam_policy_document.codebuild_kms.json
}

data "aws_iam_policy_document" "codebuild_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codebuild.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "codebuild" {
  for_each = local.codebuild_runner_config

  name                  = "${each.value.name}-role"
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary
  description           = "Role for ${each.value.name} CodeBuild Runner"
  assume_role_policy    = data.aws_iam_policy_document.codebuild_assume_role.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "codebuild" {
  for_each = merge([for runner in keys(local.codebuild_runner_config) : {
    "${runner}-logs"   = { role = aws_iam_role.codebuild[runner].name, policy = aws_iam_policy.codebuild_logs[runner].arn }
    "${runner}-github" = { role = aws_iam_role.codebuild[runner].name, policy = aws_iam_policy.codebuild_github[runner].arn }
    "${runner}-ecr"    = { role = aws_iam_role.codebuild[runner].name, policy = aws_iam_policy.codebuild_ecr[runner].arn }
    "${runner}-kms"    = { role = aws_iam_role.codebuild[runner].name, policy = aws_iam_policy.codebuild_kms[runner].arn }
  }]...)

  role       = each.value.role
  policy_arn = each.value.policy
}
