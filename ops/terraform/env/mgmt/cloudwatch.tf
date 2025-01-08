# CloudWatch Log Group for the "CI - Update OPS Infrastructure" Terraform plan/apply logs
resource "aws_cloudwatch_log_group" "gha_ci_ops_infra" {
  name       = "/bfd/${local.env}/gha/ci-ops-infra"
  kms_key_id = data.aws_kms_key.cmk.arn
}
