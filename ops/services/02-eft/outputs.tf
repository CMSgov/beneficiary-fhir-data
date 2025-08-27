locals {
  ssm_prefix = "/bfd/${local.env}/${local.service}/tf-outputs"

  outputs_map = length(local.eft_partners_with_outbound_enabled) > 0 ? { for key, value in {
    outbound_bfd_sns_topic_name      = one(module.topic_outbound_notifs[*].topic.name)
    outbound_bfd_sns_topic_arn       = one(module.topic_outbound_notifs[*].topic.arn)
    outbound_partner_sns_topic_names = values(module.topic_outbound_partner_notifs)[*].topic.name
    outbound_lambda_name             = one(aws_lambda_function.sftp_outbound_transfer[*].function_name)
    outbound_lambda_dlq_name         = one(aws_sqs_queue.sftp_outbound_transfer_dlq[*].name)
  } : key => jsonencode(value) } : {}
}

resource "aws_ssm_parameter" "outputs" {
  for_each = local.outputs_map

  name           = "${local.ssm_prefix}/${each.key}"
  tier           = "Intelligent-Tiering"
  type           = "String"
  insecure_value = each.value

  tags = {
    tf_output      = true
    auto_generated = true
  }
}
