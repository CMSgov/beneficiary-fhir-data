resource "aws_iam_role" "logs" {
  name = "${local.full_name}-logs"

  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "transfer.amazonaws.com"
          }
        },
      ]
      Version = "2012-10-17"
    }
  )
  force_detach_policies = false
  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AWSTransferLoggingAccess",
  ]
  max_session_duration = 3600
  path                 = "/"
}
