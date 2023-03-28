resource "aws_sqs_queue" "jenkins_run_job_queue" {
  name                      = "bfd-${local.env}-run-jenkins-job"
  receive_wait_time_seconds = 20
  kms_master_key_id         = local.kms_key_id
}
