data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "cmk" {
  key_id = var.kms_key_alias
}

data "archive_file" "src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.lambda_src}.py")
    filename = "${local.lambda_src}.py"
  }
}
