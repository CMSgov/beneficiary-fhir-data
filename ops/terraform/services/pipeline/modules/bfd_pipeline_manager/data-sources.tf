data "aws_region" "current" {}

data "aws_s3_bucket" "etl" {
  bucket = var.etl_bucket_id
}

data "archive_file" "lambda_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/bfd_pipeline_manager.zip"

  source {
    content  = file("${path.module}/lambda_src/pipeline_manager.py")
    filename = "pipeline_manager.py"
  }
}

data "aws_sqs_queue" "jenkins_job_queue" {
  name = "bfd-mgmt-run-jenkins-job"
}

