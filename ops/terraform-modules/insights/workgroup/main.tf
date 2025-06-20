data "aws_caller_identity" "current" {}

data "aws_kms_key" "bucket_cmk" {
  key_id      = var.bucket_cmk
}

data "aws_s3_bucket" "bucket" {
  bucket      = var.bucket
}

resource "aws_s3_bucket_object" "folder" {
  bucket        = data.aws_s3_bucket.bucket.id
  content_type  = "application/x-directory"
  key           = "workgroups/${var.name}/"
}

resource "aws_athena_workgroup" "main" {
  name        = var.name
  tags        = var.tags
  depends_on  = [aws_s3_bucket_object.folder]

  configuration {
    enforce_workgroup_configuration    = true
    publish_cloudwatch_metrics_enabled = true

    result_configuration {
      output_location     = "s3://${var.bucket}/workgroups/${var.name}/"

      encryption_configuration {
        encryption_option = "SSE_KMS"
        kms_key_arn       = data.aws_kms_key.bucket_cmk.arn
      }
    }
  }
}