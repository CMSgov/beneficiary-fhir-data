resource "aws_iam_instance_profile" "instance_profile" {
  name = "bfd-${var.env_config.env}-${var.name}-profile"
  role = aws_iam_role.instance.name
}

# Allow access to passed in S3 buckets and the standard EC2 role things
#
resource "aws_iam_role" "instance" {
  name = "bfd-${var.env_config.env}-${var.name}-role"
  path = "/"

  assume_role_policy = <<-EOF
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
      %{ for arn in var.s3_bucket_arns }
      ,{
        "Action": "s3:*",
        "Effect": "Allow",
        "Resource": ["${arn}"]
      }
      %{ endfor }
    ]
  }
  EOF
}