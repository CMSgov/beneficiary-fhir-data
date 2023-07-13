resource "aws_iam_role" "alert_lambda_scheduler" {
  name        = local.alert_lambda_scheduler_name
  path        = "/"
  description = "Role for ${local.alert_lambda_scheduler_name} Lambda"

  assume_role_policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Action = "sts:AssumeRole",
          Effect = "Allow",
          Principal = {
            Service = "lambda.amazonaws.com"
          }
        }
      ]
    }
  )

  force_detach_policies = true
}
