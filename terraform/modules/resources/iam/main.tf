resource "aws_iam_instance_profile" "instance_profile" {
  name = "bfd-${var.env_config.env}-app-profile"
  role = aws_iam_role.instance.name
}

resource "aws_iam_role" "instance" {
  name = "bfd-${var.env_config.env}-app-role"
  path = "/"

  assume_role_policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "sts:AssumeRole",
            "Principal": {
               "Service": "ec2.amazonaws.com"
            },
            "Effect": "Allow",
            "Sid": ""
        }
    ]
}
EOF
}
