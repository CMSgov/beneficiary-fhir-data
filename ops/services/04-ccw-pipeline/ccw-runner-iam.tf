data "aws_iam_policy_document" "ccw_runner_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.ccw_runner.arn}:*"]
  }
}

resource "aws_iam_policy" "ccw_runner_logs" {
  name = "${local.ccw_runner_lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.ccw_runner_lambda_full_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.ccw_runner_logs.json
}

data "aws_iam_policy_document" "ccw_runner_ssm" {
  statement {
    actions = ["ssm:GetParameter"]
    resources = [
      "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/${local.service}/sensitive/db/username",
      "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/${local.service}/sensitive/db/password"
    ]
  }
}

resource "aws_iam_policy" "ccw_runner_ssm" {
  name        = "${local.ccw_runner_lambda_full_name}-ssm"
  path        = local.iam_path
  description = "Grants permission for the ${local.ccw_runner_lambda_full_name} Lambda to get relevant SSM parameters"
  policy      = data.aws_iam_policy_document.ccw_runner_ssm.json
}

data "aws_iam_policy_document" "ccw_runner_kms" {
  statement {
    sid = "AllowEncryptionAndDecryptionOfMasterKeys"
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "ccw_runner_kms" {
  name = "${local.ccw_runner_lambda_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permission for the ${local.ccw_runner_lambda_full_name} Lambda to decrypt config KMS ",
    "keys and encrypt and decrypt master KMS keys for ${local.env}"
  ])

  policy = data.aws_iam_policy_document.ccw_runner_kms.json
}

data "aws_iam_policy_document" "ccw_runner_s3" {
  statement {
    actions   = ["s3:ListBucket"]
    resources = [module.bucket_ccw.bucket.arn]
  }
}

resource "aws_iam_policy" "ccw_runner_s3" {
  name        = "${local.ccw_runner_lambda_full_name}-s3"
  path        = local.iam_path
  description = "Grants permission for the ${local.ccw_runner_lambda_full_name} Lambda to list objects in the ${module.bucket_ccw.bucket.bucket} Bucket"
  policy      = data.aws_iam_policy_document.ccw_runner_s3.json
}

data "aws_iam_policy_document" "ccw_runner_ecs" {
  statement {
    sid       = "AllowListTasksIn${title(replace(local.env, "-", ""))}Cluster"
    actions   = ["ecs:ListTasks"]
    resources = ["*"]

    condition {
      test     = "ArnEquals"
      variable = "ecs:cluster"
      values   = [data.aws_ecs_cluster.main.arn]
    }
  }

  statement {
    sid       = "AllowDescribeAndTagTasksIn${title(replace(local.env, "-", ""))}Cluster"
    actions   = ["ecs:DescribeTasks", "ecs:TagResource"]
    resources = ["${replace(data.aws_ecs_cluster.main.arn, ":cluster", ":task")}/*"]

    condition {
      test     = "ArnEquals"
      variable = "ecs:cluster"
      values   = [data.aws_ecs_cluster.main.arn]
    }
  }

  statement {
    sid       = "AllowRunPipelineCCWEcsTaskIn${title(replace(local.env, "-", ""))}Cluster"
    actions   = ["ecs:RunTask"]
    resources = [aws_ecs_task_definition.ccw.arn]

    condition {
      test     = "ArnEquals"
      variable = "ecs:cluster"
      values   = [data.aws_ecs_cluster.main.arn]
    }
  }
}

resource "aws_iam_policy" "ccw_runner_ecs" {
  name        = "${local.ccw_runner_lambda_full_name}-ecs"
  path        = local.iam_path
  description = "Grants permission for the ${local.ccw_runner_lambda_full_name} Lambda to list and tag ECS tasks and run ${local.service} ECS tasks"
  policy      = data.aws_iam_policy_document.ccw_runner_ecs.json
}

data "aws_iam_policy_document" "ccw_runner_iam" {
  statement {
    sid       = "AllowPassRoleToPipelineExecutionRole"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.ccw_execution.arn, aws_iam_role.ccw_task.arn]
  }
}

resource "aws_iam_policy" "ccw_runner_iam" {
  name        = "${local.ccw_runner_lambda_full_name}-iam"
  path        = local.iam_path
  description = "Grants permission for the ${local.ccw_runner_lambda_full_name} to Pass IAM Role to ${local.service} Roles"
  policy      = data.aws_iam_policy_document.ccw_runner_iam.json
}

resource "aws_iam_role" "ccw_runner" {
  name                  = local.ccw_runner_lambda_full_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.ccw_runner_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["lambda"].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "ccw_runner" {
  for_each = {
    logs = aws_iam_policy.ccw_runner_logs.arn
    ssm  = aws_iam_policy.ccw_runner_ssm.arn
    kms  = aws_iam_policy.ccw_runner_kms.arn
    s3   = aws_iam_policy.ccw_runner_s3.arn
    ecs  = aws_iam_policy.ccw_runner_ecs.arn
    iam  = aws_iam_policy.ccw_runner_iam.arn
    vpc  = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
  }

  role       = aws_iam_role.ccw_runner.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "ccw_runner_scheduler_lambda" {
  statement {
    actions   = ["lambda:InvokeFunction"]
    resources = [aws_lambda_function.ccw_runner.arn]
  }
}

resource "aws_iam_policy" "ccw_runner_scheduler_lambda" {
  name = "${local.ccw_runner_lambda_full_name}-scheduler-lambda"
  path = local.iam_path
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.ccw_runner_lambda_full_name} Lambda"
  ])

  policy = data.aws_iam_policy_document.ccw_runner_scheduler_lambda.json
}

resource "aws_iam_role" "ccw_runner_scheduler" {
  name                 = "${local.ccw_runner_lambda_full_name}-scheduler"
  path                 = local.iam_path
  permissions_boundary = local.permissions_boundary_arn
  description = join("", [
    "Role for EventBridge Scheduler allowing permissions to invoke the ",
    "${local.ccw_runner_lambda_full_name} Lambda"
  ])
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["scheduler"].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "ccw_runner_scheduler" {
  role       = aws_iam_role.ccw_runner_scheduler.name
  policy_arn = aws_iam_policy.ccw_runner_scheduler_lambda.arn
}
