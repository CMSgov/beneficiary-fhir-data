
resource "aws_iam_role" "adot_collector_role" {
  name = "${local.full_name}-adot-collector-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        },
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_policy" "adot_xray_policy" {
  name        = "xray_policy"
  description = "Policy to allow X-Ray access"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Eﬀect = "Allow"
        Action = [
          "xray:PutTelemetryRecords",
          "xray:PutTraceSegments",
        ]
        Resource = "*"
      },
    ]
  })
}

resource "aws_iam_role_policy_attachment" "adot_core_policy" {
  role       = aws_iam_role.adot_collector_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchFullAccess"
}

resource "aws_iam_role_policy_attachment" "adot_xray_policy" {
  role       = aws_iam_role.adot_collector_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_iam_role_policy_attachment" "attach_xray_policy" {
  policy_arn = aws_iam_policy.adot_xray_policy.arn
  role       = aws_iam_role.adot_collector_role.name
}
