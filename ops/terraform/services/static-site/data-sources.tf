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

# data "aws_route53_zone" "this" {
#   name         = nonsensitive(data.aws_ssm_parameter.zone_name.value)
#   private_zone = nonsensitive(data.aws_ssm_parameter.zone_is_private.value)
# }

data "aws_iam_policy_document" "cfbucket_kms_key_policy" {
  statement {
    actions   = ["kms:*"]
    effect    = "Allow"
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
    effect    = "Allow"
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

data "aws_iam_policy_document" "cf_bucket_policy" { 
  statement {

    sid = "AllowCloudFrontServicePrincipal"
    effect = "Allow"

    principals {
      type = "Service" 
      identifiers = ["cloudfront.amazonaws.com"] 
    }
    
    principals {
      type = "AWS"
      identifiers = [aws_cloudfront_origin_access_identity.static_site_identity.iam_arn] 
    }
    
    principals {
      type = "AWS"
      identifiers = ["arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity ${aws_cloudfront_origin_access_identity.static_site_identity.id}"]
    }
    
    actions =  ["s3:GetObject"]  ##, "s3:ListBucket"]
    resources = [
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.bucket}/*"
    ]
  }
  
  statement {
    sid = "JenkinsRWObjects"
    effect = "Allow"
    principals { 
      type = "AWS"
      identifiers = ["*"]
    }
    actions = ["s3:*"]
    resources = [
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.bucket}",
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.bucket}/*"
    ]
    condition {
      test = "ArnEquals"
      variable = "AWS:UserId"
      values = [ "arn:aws:iam::${local.account_id}:role/cloudbees-jenkins" ]
    }
  }

  statement {
    sid     = "AllowKMSAccess"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]
    resources = [
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.bucket}",
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.bucket}/*"
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
    sid = "OnlySecureTransport"
    actions = ["s3:*"]
    condition {
        test = "Bool"
        variable = "aws:SecureTransport"
        values = [ "false" ]
    }
    effect = "Deny"
    principals {
       type = "*"
       identifiers = ["*"]
    }
    resources = [
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.bucket}",
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.bucket}/*"
    ]
  }
}


data "aws_iam_policy_document" "cf_logging_policy" {
  statement {
    sid = "AllowCloudFrontServicePrincipal"

    effect = "Allow"
    principals {
        type = "Service"
        identifiers = ["cloudfront.amazonaws.com"]
    }
    actions =  ["s3:*"] # ["s3:PutObject"]
    resources = [ 
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_logging.bucket}",
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_logging.bucket}/*"
    ]
    # condition {
    #   test = "ForAnyValue:StringEquals"
    #   variable = "AWS:SourceAccount" 
    #   values = [data.aws_caller_identity.current.account_id]
    # }
    condition {
      test = "StringEquals"
      variable = "AWS:SourceArn" 
      values = [aws_cloudfront_distribution.static_site_distribution.arn]
    }
  }
  statement {
    sid = "OnlySecureTransport"
    actions = ["s3:*"]
    condition {
        test = "Bool"
        variable = "aws:SecureTransport"
        values = [ "false" ]
    }
    effect = "Deny"
    principals {
       type = "*"
       identifiers = ["*"]
    }
    resources = [
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_logging.bucket}",
      "arn:aws:s3:::${aws_s3_bucket.cloudfront_logging.bucket}/*"
    ]
  }
}

# data "aws_acm_certificate" "env_issued" {
#   domain      = "${local.static_cf_alias}"
#   statuses    = ["ISSUED"]
#   types       = ["IMPORTED"]
#   most_recent = true
# }

data "aws_route53_zone" "vpc_root" {
  name         = local.root_domain_name
  private_zone = true
}
