resource "aws_iam_instance_profile" "instance_profile" {
  name = "bfd-${var.env_config.env}-${var.name}-profile"
  role = aws_iam_role.instance.name
}

# Create roles for our EC2 instances
#
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
    ]
  }
  EOF
}

resource "aws_iam_role_policy" "s3_policy" {
  count = length(var.s3_bucket_arns) > 0 ? 1 : 0
  name = "bfd-${var.env_config.env}-${var.name}-s3-policy"
  role = aws_iam_role.instance.id
  
  policy = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      %{ for arn in var.s3_bucket_arns }
      {
        "Action": "s3:*",
        "Effect": "Allow",
        "Resource": ["${arn}/*"]
      }
      %{ endfor }
    ]
  }
  EOF
}
