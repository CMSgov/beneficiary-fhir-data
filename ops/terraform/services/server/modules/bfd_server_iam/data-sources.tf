data "aws_caller_identity" "current" {}

data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${local.env}-cmk"
}

data "aws_kms_key" "mgmt_key" {
  key_id = "alias/bfd-mgmt-cmk"
}

data "aws_iam_policy" "cloudwatch_agent_policy" {
  arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# AWS CloudWatch agent needs extra IAM permissions for x-ray
data "aws_iam_policy" "cloudwatch_xray_policy" {
  arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}
