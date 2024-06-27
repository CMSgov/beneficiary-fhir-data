# Creates a private static zone for the environment
data "aws_canonical_user_id" "current" {}

# data "aws_route53_zone" "root_zone" {
#   vpc_id = data.aws_vpc.main.id
# }

data "aws_ssm_parameter" "root_domain" {
  name = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_domain"
}

locals {
#  root_domain_name      = data.aws_route53_zone.root_zone.name
  root_domain_name      = data.aws_ssm_parameter.root_domain.value
  static_cf_bucket_name = "bfd-${terraform.workspace}-cf-${local.account_id}"
  static_cf_alias       = "${terraform.workspace}.static.${local.root_domain_name}"
}

# resource "aws_route53_zone" "static" {
#   name          = "${local.env}.${local.root_domain_name}"
#   comment       = "BFD static-site zone for ${local.env}"
#   force_destroy = true

#   # VPC is only valid for private zones
#   dynamic "vpc" {
#     for_each = ["dummy"]
#     content {
#       vpc_id = data.aws_vpc.main.id
#     }
#   }
# }

data "aws_route53_zone" "vpc_root" {
  name         = local.root_domain_name
  private_zone = true
}

resource "aws_route53_record" "static_env" {
  zone_id = data.aws_route53_zone.vpc_root.zone_id
  name    = local.static_cf_alias
  type    = "A"
  
  alias {
    name    = aws_cloudfront_distribution.static_site_distribution.domain_name
    zone_id = aws_cloudfront_distribution.static_site_distribution.hosted_zone_id
    
    evaluate_target_health = true
  }

}

resource "aws_s3_bucket" "cloudfront_bucket" {
  bucket        = local.is_ephemeral_env ? null : local.static_cf_bucket_name
  bucket_prefix = local.is_ephemeral_env ? null : "static"

  tags = {
    Layer = "static-${local.layer}",
    role  = local.legacy_service
  }
}

resource "aws_s3_bucket_ownership_controls" "cloudfront_bucket" {
  bucket = aws_s3_bucket.cloudfront_bucket.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "cloudfront_bucket" {
  bucket     = aws_s3_bucket.cloudfront_bucket.id
  depends_on = [ aws_s3_bucket_ownership_controls.cloudfront_bucket ]
  acl        = "private"
}

resource "aws_s3_bucket_website_configuration" "static" {
  bucket = aws_s3_bucket.cloudfront_bucket.id

  index_document {
    suffix = "index"
  }

  error_document {
    key = "error"
  }

}

# block public access to the bucket
resource "aws_s3_bucket_public_access_block" "cloudfront_bucket" {
  bucket                  = aws_s3_bucket.cloudfront_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cloudfront_bucket" {
  bucket = aws_s3_bucket.cloudfront_bucket.id
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.kms_key_id
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_object" "index" {
  bucket  = aws_s3_bucket.cloudfront_bucket.id
  key     = "index"
  
  content_type = "text/plain"
  content      = <<EOF
<!DOCTYPE html>
<html>
<head>${terraform.workspace} Index page</head>
<body>
<p>Placeholder page for ${terraform.workspace} Static Site</p>
</body>
</html>
EOF

  lifecycle {
    ignore_changes = [ content, tags ]
  }
}

resource "aws_s3_object" "error" {
  bucket  = aws_s3_bucket.cloudfront_bucket.id
  key     = "error"
  
  content_type = "text/plain"
  content      = <<EOF
<!DOCTYPE html>
<html>
<head>${terraform.workspace} Error page</head>
<body>
<p>Placeholder Error page for ${terraform.workspace} Static Site</p>
</body>
</html>
EOF

  lifecycle {
    ignore_changes = [ content, tags ]
  }
}

resource "aws_s3_bucket_logging" "cloudfront_bucket" {
  count = local.is_ephemeral_env ? 0 : 1

  bucket = aws_s3_bucket.cloudfront_bucket.id

  target_bucket = local.logging_bucket
  target_prefix = "static-${local.legacy_service}_s3_access_logs/"
}
# leveraging env/mgmt/s3-base.tf as starting point
resource "aws_s3_bucket_policy" "cloudfront_bucket" {
  bucket = aws_s3_bucket.cloudfront_bucket.id
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Id": "PutObjPolicy",
    "Statement": [
        {
          "Sid": "AllowCloudFrontServicePrincipal",
          "Effect": "Allow",
          "Principal": {
              "Service": "cloudfront.amazonaws.com"
          },
          "Action": "s3:GetObject",
          "Resource": "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.arn}/*",
          "Condition": {
              "StringEquals": {
                "AWS:SourceArn": "${aws_cloudfront_distribution.static_site_distribution.arn}"
              }
          }
        }
    ]
}
EOF
}

resource "aws_s3_bucket" "cloudfront_logging" {
  bucket = local.logging_bucket
  tags = {
    Layer = "static-${local.layer}"
    role  = "logs"
  }
}

resource "aws_s3_bucket_public_access_block" "cloudfront_logging" {
  bucket                  = aws_s3_bucket.cloudfront_logging.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cloudfront_logging" {
  bucket = aws_s3_bucket.cloudfront_logging.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

## using "arn:aws:iam::aws:policy/CloudFrontFullAccess" verbatim 2024-06-27
resource "aws_s3_bucket_policy" "cloudfront_logging" {
  bucket = aws_s3_bucket.cloudfront_logging.id

  policy = <<POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "cfflistbuckets",
            "Action": [
                "s3:ListAllMyBuckets"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:s3:::*",
            "Principal": {
              "Service": "cloudfront.amazonaws.com"
            }
        },
        {
            "Sid": "cffullaccess",
            "Action": [
                "acm:ListCertificates",
                "cloudfront:*",
                "cloudfront-keyvaluestore:*",
                "iam:ListServerCertificates",
                "waf:ListWebACLs",
                "waf:GetWebACL",
                "wafv2:ListWebACLs",
                "wafv2:GetWebACL",
                "kinesis:ListStreams"
            ],
            "Effect": "Allow",
            "Resource": "*",
            "Principal": {
              "Service": "cloudfront.amazonaws.com"
            }
        },
        {
            "Sid": "cffdescribestream",
            "Action": [
                "kinesis:DescribeStream"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:kinesis:*:*:*",
            "Principal": {
              "Service": "cloudfront.amazonaws.com"
            }
        },
        {
            "Sid": "cfflistroles",
            "Action": [
                "iam:ListRoles"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:iam::*:*",
            "Principal": {
              "Service": "cloudfront.amazonaws.com"
            }

        }
    ]
}
POLICY
}

resource "aws_cloudfront_distribution" "static_site_distribution" {
  origin {
    domain_name = aws_s3_bucket.cloudfront_bucket.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.cloudfront_bucket.id}"
    origin_access_control_id = aws_cloudfront_origin_access_control.static_site_control.id

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.static_site_identity.cloudfront_access_identity_path
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "CloudFront distribution for static site ${terraform.workspace}"
  default_root_object = "index"

  aliases = [local.root_domain_name]

  logging_config {
    include_cookies = false
    bucket = aws_s3_bucket.cloudfront_logging.id
    prefix = local.is_ephemeral_env ? null : "static"
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${aws_s3_bucket.cloudfront_bucket.id}"
    viewer_protocol_policy = "redirect-to-https"

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  restrictions {
    geo_restriction {
      restriction_type = "whitelist"
      locations        = ["US"]
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "StaticSite-CloudFront-${terraform.workspace}"
    Layer = local.layer
    role  = "static-site"
  }
}

resource "aws_cloudfront_origin_access_identity" "static_site_identity" {
  comment = "Origin access identity for static site ${terraform.workspace}"
}

resource "aws_cloudfront_origin_access_control" "static_site_control" {
  name                              = "static_site_control-${terraform.workspace}"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}
