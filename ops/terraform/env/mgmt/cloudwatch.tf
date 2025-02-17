# CloudWatch Log Group for the "CI - Update OPS Infrastructure" Terraform plan/apply logs
resource "aws_cloudwatch_log_group" "cd-terraform-mgmt-deploy" {
  name       = "/bfd/${local.env}/gha/cd-terraform-mgmt-deploy"
  kms_key_id = data.aws_kms_key.cmk.arn
}
