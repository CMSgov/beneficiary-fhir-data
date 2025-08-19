locals {
  outbound_lambda_name            = "sftp-outbound-transfer"
  outbound_lambda_full_name       = "${local.full_name}-${local.outbound_lambda_name}"
  outbound_lambda_repository_name = coalesce(var.outbound_lambda_repository_override, "bfd-platform-${local.service}-${local.outbound_lambda_name}-lambda")
  outbound_lambda_version         = coalesce(var.outbound_lambda_version_override, local.bfd_version)
  outbound_lambda_src             = replace(local.outbound_lambda_name, "-", "_")
  outbound_lambda_timeout         = 450
}

data "aws_ecr_repository" "sftp_outbound_transfer" {
  name = local.outbound_lambda_repository_name
}

data "aws_ecr_image" "sftp_outbound_transfer" {
  repository_name = data.aws_ecr_repository.sftp_outbound_transfer.name
  image_tag       = local.bfd_version
}

data "aws_iam_policy_document" "topic_outbound_pending_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_enabled)

  statement {
    sid       = "Allow_Publish_from_S3"
    actions   = ["SNS:Publish"]
    resources = [local.topic_arn_placeholder]

    principals {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = [module.bucket_eft.bucket.arn]
    }
  }
}

module "topic_outbound_pending_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_enabled)

  source = "../../terraform-modules/general/logging-sns-topic"

  topic_name                   = "${local.full_name}-outbound-pending-s3-${each.key}"
  additional_topic_policy_docs = [data.aws_iam_policy_document.topic_outbound_pending_s3_notifs[each.key].json]

  iam_path                 = local.iam_path
  permissions_boundary_arn = local.permissions_boundary_arn

  kms_key_arn = local.env_key_arn

  sqs_sample_rate    = 100
  lambda_sample_rate = 100
}

module "topic_outbound_notifs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  source = "../../terraform-modules/general/logging-sns-topic"

  topic_name = local.outbound_notifs_topic_prefix

  iam_path                 = local.iam_path
  permissions_boundary_arn = local.permissions_boundary_arn

  kms_key_arn = local.env_key_arn

  sqs_sample_rate    = 100
  lambda_sample_rate = 100
}

data "aws_iam_policy_document" "topic_outbound_partner_notifs" {
  for_each = toset(local.eft_partners_with_outbound_notifs)

  dynamic "statement" {
    for_each = {
      for index, target in local.eft_partners_config[each.key].outbound.notification_targets
      : index => target
    }

    content {
      sid       = "Allow_Subscribe_from_${each.key}_${index}"
      actions   = ["SNS:Subscribe", "SNS:Receive"]
      resources = [local.topic_arn_placeholder]

      principals {
        type        = "AWS"
        identifiers = [each.value.principal]
      }

      condition {
        test     = "StringEquals"
        variable = "sns:Protocol"
        values   = [each.value.protocol]

      }

      condition {
        test     = "ForAllValues:StringEquals"
        variable = "sns:Endpoint"
        values   = [each.value.arn]
      }
    }
  }
}

module "topic_outbound_partner_notifs" {
  for_each = toset(local.eft_partners_with_outbound_notifs)

  source = "../../terraform-modules/general/logging-sns-topic"

  topic_name = "${local.outbound_notifs_topic_prefix}-${each.key}"

  iam_path                 = local.iam_path
  permissions_boundary_arn = local.permissions_boundary_arn

  kms_key_arn = local.env_key_arn

  sqs_sample_rate    = 100
  lambda_sample_rate = 100
}

data "aws_iam_policy_document" "sftp_outbound_transfer_invoke" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  statement {
    sid       = "AllowSNSSendMessage"
    actions   = ["sqs:SendMessage"]
    resources = aws_sqs_queue.sftp_outbound_transfer_invoke[*].arn

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    condition {
      test     = "ForAnyValue:ArnEquals"
      variable = "aws:SourceArn"
      values   = values(module.topic_outbound_pending_s3_notifs)[*].topic.arn
    }
  }
}

resource "aws_sqs_queue" "sftp_outbound_transfer_invoke" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name              = "${local.outbound_lambda_full_name}-sqs"
  kms_master_key_id = local.env_key_arn

  visibility_timeout_seconds = local.outbound_lambda_timeout
}

resource "aws_sqs_queue_policy" "sftp_outbound_transfer_invoke" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  queue_url = one(aws_sqs_queue.sftp_outbound_transfer_invoke[*].id)
  policy    = one(data.aws_iam_policy_document.sftp_outbound_transfer_invoke[*].json)
}

resource "aws_sqs_queue" "sftp_outbound_transfer_dlq" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name                      = "${local.outbound_lambda_full_name}-dlq"
  kms_master_key_id         = local.env_key_arn
  message_retention_seconds = 14 * 24 * 60 * 60 # 14 days, in seconds, which is the maximum
}

resource "aws_sqs_queue_redrive_allow_policy" "sftp_outbound_transfer" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  queue_url = one(aws_sqs_queue.sftp_outbound_transfer_dlq[*].id)
  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue",
    sourceQueueArns   = aws_sqs_queue.sftp_outbound_transfer_invoke[*].arn
  })
}

resource "aws_sqs_queue_redrive_policy" "sftp_outbound_transfer" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  queue_url = one(aws_sqs_queue.sftp_outbound_transfer_invoke[*].id)
  redrive_policy = jsonencode({
    deadLetterTargetArn = one(aws_sqs_queue.sftp_outbound_transfer_dlq[*].arn)
    maxReceiveCount     = 4
  })
}

resource "aws_sns_topic_subscription" "sftp_outbound_transfer" {
  for_each = toset(local.eft_partners_with_outbound_enabled)

  topic_arn = module.topic_outbound_pending_s3_notifs[each.key].topic.arn
  protocol  = "sqs"
  endpoint  = one(aws_sqs_queue.sftp_outbound_transfer_invoke[*].arn)
}

resource "aws_cloudwatch_log_group" "sftp_outbound_transfer" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name         = "/aws/lambda/${local.outbound_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "sftp_outbound_transfer" {
  depends_on = [aws_iam_role_policy_attachment.sftp_outbound_transfer]
  count      = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  function_name = local.outbound_lambda_full_name

  description = join("", [
    "Invoked when participating Peering Partners upload files to their corresponding outbound ",
    "pending files folder in the ${local.full_name} Bucket. This Lambda will then upload those ",
    "file(s) via CMS EFT SFTP if they are recognized and valid."
  ])

  kms_key_arn      = local.env_key_arn
  image_uri        = data.aws_ecr_image.sftp_outbound_transfer.image_uri
  source_code_hash = trimprefix(data.aws_ecr_image.sftp_outbound_transfer.id, "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"
  memory_size      = 5120
  timeout          = local.outbound_lambda_timeout

  logging_config {
    log_group  = one(aws_cloudwatch_log_group.sftp_outbound_transfer[*].name)
    log_format = "Text"
  }

  reserved_concurrent_executions = 1

  tags = {
    Name = local.outbound_lambda_full_name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT   = local.env
      BUCKET            = local.full_name
      BUCKET_ROOT_DIR   = local.inbound_sftp_s3_home_dir
      BFD_SNS_TOPIC_ARN = one(module.topic_outbound_notifs[*].topic.arn)
      SNS_TOPIC_ARNS_BY_PARTNER_JSON = jsonencode(
        {
          for partner in local.eft_partners_with_outbound_notifs
          : partner => module.topic_outbound_partner_notifs[partner].topic.arn
        }
      )
    }
  }

  role = one(aws_iam_role.sftp_outbound_transfer[*].arn)

  vpc_config {
    security_group_ids = aws_security_group.sftp_outbound_transfer[*].id
    subnet_ids         = local.subnets[*].id
  }
}

resource "aws_lambda_permission" "sftp_outbound_transfer_sqs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  statement_id   = "${local.outbound_lambda_full_name}-allow-sqs"
  action         = "lambda:InvokeFunction"
  function_name  = one(aws_lambda_function.sftp_outbound_transfer[*].function_name)
  principal      = "sqs.amazonaws.com"
  source_arn     = one(aws_sqs_queue.sftp_outbound_transfer_invoke[*].arn)
  source_account = local.account_id
}

resource "aws_lambda_event_source_mapping" "sftp_outbound_transfer" {
  count      = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0
  depends_on = [aws_iam_role_policy_attachment.sftp_outbound_transfer]

  event_source_arn = one(aws_sqs_queue.sftp_outbound_transfer_invoke[*].arn)
  function_name    = one(aws_lambda_function.sftp_outbound_transfer[*].function_name)
  batch_size       = 1
}

resource "aws_lambda_function_event_invoke_config" "sftp_outbound_transfer" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  function_name          = one(aws_lambda_function.sftp_outbound_transfer[*].function_name)
  maximum_retry_attempts = 2

  # If the Lambda exhausts all of its retry attempts, we want failing events to land into a DLQ such
  # that responding engineers can analyze the event and retry, if necessary
  destination_config {
    on_failure {
      destination = one(aws_sqs_queue.sftp_outbound_transfer_dlq[*].arn)
    }
  }
}

resource "aws_security_group" "sftp_outbound_transfer" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name        = local.outbound_lambda_full_name
  description = "Allow ${local.outbound_lambda_full_name} to the ${local.env} VPC"
  vpc_id      = local.vpc_id
  tags        = { Name = "${local.outbound_lambda_full_name}" }

  # Unfortunately, it seems restricting egress beyond just the VPC requires that the Lambda uses
  # PrivateLink VPC Endpoints (or a VPC Gateway) to use any boto3 clients. That is beyond the scope
  # of this Terraservice, but should be considered for future work
  # FUTURE: Investigate tightening the egress rule
  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}
