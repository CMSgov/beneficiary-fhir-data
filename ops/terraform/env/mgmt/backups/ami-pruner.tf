locals {
  ami_pruner_cfg = {
    timeout_seconds           = 900
    engine                    = "python3.11"
    memory_size               = 128
    cron_schedule             = "0 2 * * ? *" # 2am UTC
    log_retention_days        = 30
    log_group                 = "/aws/lambda/bfd-${local.env}-backups-ami-pruner"
    log_level                 = 20 # 10 = DEBUG, 20 = INFO, 30 = WARNING, 40 = ERROR, 50 = CRITICAL
    retention_policy_ssm_path = "/bfd/${local.env}/common/nonsensitive/backups/ami"
  }
}

# lambda function
resource "aws_lambda_function" "ami_pruner" {
  function_name = "bfd-${local.env}-backups-ami-pruner"
  description   = "Prunes Platinum/App AMIs falling outside of their respective retention policies."
  runtime       = local.ami_pruner_cfg.engine
  timeout       = local.ami_pruner_cfg.timeout_seconds
  memory_size   = local.ami_pruner_cfg.memory_size
  publish       = "true"

  role             = aws_iam_role.ami_pruner.arn
  filename         = "${path.module}/lambda-src/ami_pruner/ami_pruner.zip"
  source_code_hash = filebase64sha256(data.archive_file.ami_pruner.output_path)
  handler          = "ami_pruner.lambda_handler"

  environment {
    variables = {
      AWS_PRUNE_REGION                = local.region
      LOG_LEVEL                       = local.ami_pruner_cfg.log_level
      AMI_RETENTION_POLICIES_SSM_PATH = local.ami_pruner_cfg.retention_policy_ssm_path
    }
  }
}

# source code
data "archive_file" "ami_pruner" {
  type        = "zip"
  source_file = "${path.module}/lambda-src/ami_pruner/ami_pruner.py"
  output_path = "${path.module}/lambda-src/ami_pruner/ami_pruner.zip"
}

# cron schedule
resource "aws_cloudwatch_event_rule" "ami_pruner_schedule" {
  name                = "bfd-${local.env}-backups-ami-pruning-schedule"
  description         = "Cron schedule for running the AMI pruner"
  schedule_expression = "cron(${local.ami_pruner_cfg.cron_schedule})"
}

# trigger
resource "aws_cloudwatch_event_target" "ami_pruner_schedule" {
  rule      = aws_cloudwatch_event_rule.ami_pruner_schedule.name
  target_id = "triggerLambdaFunction"
  arn       = aws_lambda_function.ami_pruner.arn
}

# allow cloudwatch to invoke the function
resource "aws_lambda_permission" "ami_pruner_schedule" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.ami_pruner.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.ami_pruner_schedule.arn
}

# lambda log group
resource "aws_cloudwatch_log_group" "ami_pruner" {
  name              = local.ami_pruner_cfg.log_group
  retention_in_days = local.ami_pruner_cfg.log_retention_days
}

# iam role
resource "aws_iam_role" "ami_pruner" {
  name = "bfd-${local.env}-backups-ami-pruner"
  path = local.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn # Temporarily Commented out
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = ["sts:AssumeRole"]
        Effect = "Allow"
        Principal = {
          Service = ["lambda.amazonaws.com"]
        }
      },
    ]
  })
}

# lambda role permission policy
resource "aws_iam_role_policy" "ami_pruner" {
  name = "bfd-${local.env}-backups-ami-pruner"
  role = aws_iam_role.ami_pruner.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ec2:DescribeImages",
          "ec2:DescribeSnapshots",
          "ec2:DescribeInstances",
          "ec2:DescribeVolumes",
          "ec2:DescribeTags",
          "ec2:DescribeLaunchTemplate*",
        ]
        Resource = [
          "*",
        ]
        Sid = "AllowDescribe"
      },
      {
        Effect = "Allow"
        Action = [
          "ec2:DeregisterImage",
          "ec2:DeleteSnapshot",
        ]
        Resource = [
          "arn:aws:ec2:${local.region}::snapshot/*",
          "arn:aws:ec2:${local.region}::image/*"
        ]
        Sid = "AllowDeregAndDelete"
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParametersByPath",
        ]
        Resource = [
          "arn:aws:ssm:${local.region}:${local.account_id}:parameter${local.ami_pruner_cfg.retention_policy_ssm_path}",
        ]
        Sid = "AllowRetentionPolicyLookup"
      },
      {
        Effect = "Allow",
        Action = [
          "logs:CreateLogGroup",
        ],
        Resource = [
          "arn:aws:logs:${local.region}:${local.account_id}:*",
        ],
        Sid = "AllowLogGroupCreation"
        }, {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = [
          "arn:aws:logs:${local.region}:${local.account_id}:log-group:${local.ami_pruner_cfg.log_group}:*",
        ]
        Sid = "AllowLogging"
        }, {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
        ]
        Resource = [
          local.config_cmk.arn,
        ]
        Sid = "AllowKmsDecryption"
      }
    ]
  })
}
