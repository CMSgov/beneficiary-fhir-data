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

data "aws_vpc" "this" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

data "aws_subnets" "env_subnets" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.this.id]
  }
  filter {
    name   = "tag:Name"
    values = ["*${local.layer}"]
  }
}

data "aws_lb_hosted_zone_id" "static_lb" {
  region = local.region
  load_balancer_type = aws_lb.static_lb.load_balancer_type
}

data "aws_ssm_parameter" "zone_name" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_domain"
  with_decryption = true
}

data "aws_ssm_parameter" "zone_is_private" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_is_private"
  with_decryption = true
}

data "aws_iam_policy_document" "s3_static_policy" {
  depends_on = [aws_s3_bucket.static_site]
  statement {
    sid = "access-to-specific-VPCE-only"
    effect = "Allow"
    principals {
      type = "AWS"
      identifiers = ["*"]
    }
    actions = ["s3:GetObject"]
    resources = [
      "${aws_s3_bucket.static_site.arn}",
      "${aws_s3_bucket.static_site.arn}/*"
    ]
    condition {
      test     = "StringEquals"
      variable = "aws:SourceVpce"
      values   = [aws_vpc_endpoint.s3.id]
    }
  }

  statement {
    sid    = "JenkinsRWObjects"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = ["*"]
    }
    actions = ["s3:*"]
    resources = [
      "${aws_s3_bucket.static_site.arn}",
      "${aws_s3_bucket.static_site.arn}/*"
    ]
    condition {
      test     = "ArnEquals"
      variable = "AWS:UserId"
      values   = ["arn:aws:iam::${local.account_id}:role/cloudbees-jenkins"]
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
      "${aws_s3_bucket.static_site.arn}",
      "${aws_s3_bucket.static_site.arn}/*"
    ]
  }
}


data "aws_iam_policy_document" "vpce_alb_log_policy" {
  depends_on = [aws_s3_bucket.vpce_alb_logging]
  statement {
    sid = "allowELBLogging"
    effect = "Allow"
    principals {
      type = "Service"
      identifiers = ["logdelivery.elasticloadbalancing.amazonaws.com"]
    }
    actions = [
      "s3:PutBucketLogging",
      "s3:PutObject",
      "s3:GetObjectAcl",
      "s3:GetBucketAcl"
    ]
    resources = [
      "${aws_s3_bucket.vpce_alb_logging.arn}",
      "${aws_s3_bucket.vpce_alb_logging.arn}/*"
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
      "${aws_s3_bucket.vpce_alb_logging.arn}",
      "${aws_s3_bucket.vpce_alb_logging.arn}/*"
    ]
  }
}

# TODO BFD-3489
data "aws_acm_certificate" "env_issued" {
  domain      = "${local.static_site_fqdn}"
  statuses    = ["ISSUED"]
  types       = ["IMPORTED"]
  most_recent = true
}

data "aws_route53_zone" "vpc_root" {
  name         = local.root_domain_name
  private_zone = true
}
