data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_canonical_user_id" "current" {}

data "aws_kms_key" "config_cmk" {
  key_id = local.kms_config_key_alias
}

data "aws_kms_key" "data_cmk" {
  key_id = local.kms_key_alias
}

data "aws_ssm_parameters_by_path" "params" {
  for_each = toset(local.ssm_hierarchies)

  recursive       = true
  path            = each.value
  with_decryption = true
}

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

data "aws_vpc" "this" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

data "aws_ssm_parameter" "zone_name" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_domain"
  with_decryption = true
}

data "aws_ssm_parameter" "zone_is_private" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_is_private"
  with_decryption = true
}

data "aws_iam_policy_document" "cloudfront_policy" {
  ## TODO: this policy may need updated with BFD-3465
  statement {
    sid    = "AllowCloudFrontServicePrincipal"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.static_site_distribution.arn]
    }

    actions = ["s3:GetObject"]
    resources = [
      "${aws_s3_bucket.static_site.arn}/*"
    ]
  }

  statement {
    sid     = "OnlySecureTransport"
    actions = ["s3:*"]
    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
    effect = "Deny"
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    resources = [
      aws_s3_bucket.static_site.arn,
      "${aws_s3_bucket.static_site.arn}/*"
    ]
  }
}

data "aws_iam_policy_document" "cloudfront_log_policy" {
  statement {
    sid = "AllowCloudFrontServicePrincipal"

    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.cloudfront_logging.arn,
      "${aws_s3_bucket.cloudfront_logging.arn}/*"
    ]
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.static_site_distribution.arn]
    }
  }
  statement {
    sid     = "OnlySecureTransport"
    actions = ["s3:*"]
    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
    effect = "Deny"
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    resources = [
      aws_s3_bucket.cloudfront_logging.arn,
      "${aws_s3_bucket.cloudfront_logging.arn}/*"
    ]
  }
}

data "aws_route53_zone" "vpc_root" {
  name         = local.root_domain_name
  private_zone = true
}
