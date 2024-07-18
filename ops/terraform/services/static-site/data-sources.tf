data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_canonical_user_id" "current" {}

data "aws_kms_key" "config_cmk" {
  key_id = local.kms_config_key_alias
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

data "aws_iam_policy_document" "static_kms_key_policy" {
  depends_on = [aws_cloudfront_distribution.static_site_distribution, aws_cloudfront_origin_access_identity.static_site_identity]
  statement {
    actions = ["kms:*"]
    effect  = "Allow"
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"]
    }
    resources = ["*"]
  }

  statement {
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
      "kms:ReEncrypt*"
    ]
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [aws_cloudfront_origin_access_identity.static_site_identity.iam_arn]
    }
    resources = ["*"]
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.static_site_distribution.arn]
    }
  }
}

data "aws_iam_policy_document" "cloudfront_policy" {
  ## TODO: this policy may need updated with BFD-3465
  depends_on = [aws_cloudfront_origin_access_identity.static_site_identity, aws_s3_bucket.static_site]
  statement {

    sid    = "AllowCloudFrontServicePrincipal"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    principals {
      type        = "AWS"
      identifiers = [aws_cloudfront_origin_access_identity.static_site_identity.iam_arn]
    }

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity ${aws_cloudfront_origin_access_identity.static_site_identity.id}"]
    }

    actions = ["s3:GetObject"] 
    resources = [
      "${aws_s3_bucket.static_site.arn}/*"
    ]
  }

  statement {
    sid = "AllowKMSAccess"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]
    resources = [
      aws_s3_bucket.static_site.arn,
      "${aws_s3_bucket.static_site.arn}/*"
    ]

    principals {
      type        = "AWS"
      identifiers = [aws_cloudfront_origin_access_identity.static_site_identity.iam_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SecureTransport"
      values   = ["true"]
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
      aws_s3_bucket.static_site.arn,
      "${aws_s3_bucket.static_site.arn}/*"
    ]
  }
}


data "aws_iam_policy_document" "cloudfront_log_policy" {
  depends_on = [aws_s3_bucket.cloudfront_logging, aws_cloudfront_distribution.static_site_distribution]
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

## TODO - see ./r53.tf
# data "aws_acm_certificate" "env_issued" {
#   domain      = "${local.static_site_fqdn}"
#   statuses    = ["ISSUED"]
#   types       = ["IMPORTED"]
#   most_recent = true
# }

data "aws_route53_zone" "vpc_root" {
  name         = local.root_domain_name
  private_zone = true
}
