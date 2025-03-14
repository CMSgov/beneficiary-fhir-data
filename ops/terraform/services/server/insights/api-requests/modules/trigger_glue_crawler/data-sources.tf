data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-${local.seed_env}-cmk"
}

data "archive_file" "lambda_src" {
  type        = "zip"
  source_file = "${path.module}/lambda_src/trigger_glue_crawler.py"
  output_path = "${path.module}/lambda_src/trigger_glue_crawler.zip"
}


data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
