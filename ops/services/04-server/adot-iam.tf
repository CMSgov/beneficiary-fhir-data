
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

resource "aws_iam_role_policy_attachment" "adot_core_policy" {
  role       = aws_iam_role.adot_collector_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchFullAccess"
}
