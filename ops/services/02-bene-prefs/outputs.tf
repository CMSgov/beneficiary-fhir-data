
output "partner_bucket_names" {
  value = { for p in local.partners : p => module.eft_bucket[p].bucket.bucket }
}

output "partner_topic_arns" {
  value = { for p in local.partners : p => aws_sns_topic.partner_bucket_events[p].arn }
}

output "partner_queue_arns" {
  value = { for p in local.partners : p => aws_sqs_queue.partner_bucket_events[p].arn }
}
