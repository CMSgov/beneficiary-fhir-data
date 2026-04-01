data "aws_iam_policy_document" "run_idr_scheduler_lambda" {
  statement {
    actions   = ["lambda:InvokeFunction"]
    resources = [aws_lambda_function.run_idr.arn]
  }
}

resource "aws_iam_policy" "run_idr_scheduler_lambda" {
  name = "${local.run_idr_lambda_full_name}-scheduler-lambda"
  path = local.iam_path
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.run_idr_lambda_full_name} Lambda"
  ])

  policy = data.aws_iam_policy_document.run_idr_scheduler_lambda.json
}

resource "aws_iam_role" "run_idr_scheduler" {
  name                 = "${local.run_idr_lambda_full_name}-scheduler"
  path                 = local.iam_path
  permissions_boundary = local.permissions_boundary_arn
  description = join("", [
    "Role for EventBridge Scheduler allowing permissions to invoke the ",
    "${local.run_idr_lambda_full_name} Lambda"
  ])
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["scheduler"].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "run_idr_scheduler" {
  role       = aws_iam_role.run_idr_scheduler.name
  policy_arn = aws_iam_policy.run_idr_scheduler_lambda.arn
}

data "aws_iam_policy_document" "run_idr_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.run_idr.arn}:*"]
  }
}

data "aws_iam_policy_document" "run_idr_kms" {
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

resource "aws_iam_policy" "run_idr_kms" {
  name = "${local.run_idr_lambda_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permission for the ${local.run_idr_lambda_full_name} Lambda to decrypt config KMS ",
    "keys and encrypt and decrypt master KMS keys for ${local.env}"
  ])

  policy = data.aws_iam_policy_document.run_idr_kms.json
}

resource "aws_iam_policy" "run_idr_logs" {
  name = "${local.run_idr_lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.run_idr_lambda_full_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.run_idr_logs.json
}

data "aws_iam_policy_document" "run_idr_ecs" {
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
    sid       = "AllowRunPipelineIDREcsTaskIn${title(replace(local.env, "-", ""))}Cluster"
    actions   = ["ecs:RunTask"]
    resources = [aws_ecs_task_definition.idr_new.arn]

    condition {
      test     = "ArnEquals"
      variable = "ecs:cluster"
      values   = [data.aws_ecs_cluster.main.arn]
    }
  }
}

resource "aws_iam_policy" "run_idr_ecs" {
  name        = "${local.run_idr_lambda_full_name}-ecs"
  path        = local.iam_path
  description = "Grants permission for the ${local.run_idr_lambda_full_name} Lambda to list and tag ECS tasks and run ${local.service} ECS tasks"
  policy      = data.aws_iam_policy_document.run_idr_ecs.json
}

data "aws_iam_policy_document" "run_idr_iam" {
  statement {
    sid       = "AllowPassRoleToSchedulerRole"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.run_idr_scheduler.arn]
  }
}

resource "aws_iam_policy" "run_idr_iam" {
  name        = "${local.run_idr_lambda_full_name}-iam"
  path        = local.iam_path
  description = "Grants permission for the ${local.run_idr_lambda_full_name} to Pass IAM Role to ${aws_iam_role.run_idr_scheduler.name}"
  policy      = data.aws_iam_policy_document.run_idr_iam.json
}

resource "aws_iam_role" "run_idr" {
  name                  = local.run_idr_lambda_full_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.run_idr_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["lambda"].json
  force_detach_policies = true
}

data "aws_iam_policy_document" "run_idr_scheduler" {
  statement {
    sid       = "AllowCreateAndDeleteInScheduleGroup"
    actions   = ["scheduler:CreateSchedule", "scheduler:DeleteSchedule", "scheduler:GetSchedule"]
    resources = ["${replace(aws_scheduler_schedule_group.run_idr.arn, ":schedule-group", ":schedule")}/*"]
  }

  statement {
    sid       = "AllowListAllSchedules"
    actions   = ["scheduler:ListSchedules"]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "run_idr_scheduler" {
  name        = "${local.run_idr_lambda_full_name}-scheduler"
  path        = local.iam_path
  description = "Grants permission for the ${local.run_idr_lambda_full_name} Lambda to relevant EventBridge Scheduler resources"
  policy      = data.aws_iam_policy_document.run_idr_scheduler.json
}

resource "aws_iam_role_policy_attachment" "run_idr" {
  for_each = {
    logs      = aws_iam_policy.run_idr_logs.arn
    kms       = aws_iam_policy.run_idr_kms.arn
    ecs       = aws_iam_policy.run_idr_ecs.arn
    iam       = aws_iam_policy.run_idr_iam.arn
    scheduler = aws_iam_policy.run_idr_scheduler.arn
  }

  role       = aws_iam_role.run_idr.name
  policy_arn = each.value
}
