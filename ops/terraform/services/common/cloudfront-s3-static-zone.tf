# Creates a private static zone for the environment
data "aws_canonical_user_id" "current" {}

data "aws_route53_zone" "root_zone" {
  vpc_id = data.aws_vpc.main.id
}

locals {
  root_domain_name      = data.aws_route53_zone.root_zone.name
  static_cf_bucket_name = "bfd-${local.env}-cloudfront-${local.account_id}"
}

resource "aws_route53_zone" "static" {
  name          = "${local.env}.${local.root_domain_name}"
  comment       = "BFD static-site zone for ${local.env}"
  force_destroy = true

  # VPC is only valid for private zones
  dynamic "vpc" {
    for_each = ["dummy"]
    content {
      vpc_id = data.aws_vpc.main.id
    }
  }
}

resource "aws_route53_record" "static" {
  zone_id = aws_route53_zone.static.zone_id
  name    = "static.${aws_route53_zone.static.name}"
  type    = "CNAME"
  ttl     = "300"
  records = [aws_cloudfront_distribution.static_site_distribution.domain_name]
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

resource "aws_s3_bucket_object" "index" {
  bucket  = aws_s3_bucket.cloudfront_bucket.id
  key     = "index"
  
  content_type = "text/plain"
  content      = <<EOF
<!DOCTYPE html>
<html>
<head>${local.env} Index page</head>
<body>
<p>Placeholder page for ${local.env} Static Site</p>
</body>
</html>
EOF

  lifecycle {
    ignore_changes = [ values, tags ]
  }
}

resource "aws_s3_bucket_object" "error" {
  bucket  = aws_s3_bucket.cloudfront_bucket.id
  key     = "error"
  
  content_type = "text/plain"
  content      = <<EOF
<!DOCTYPE html>
<html>
<head>${local.env} Error page</head>
<body>
<p>Placeholder Error page for ${local.env} Static Site</p>
</body>
</html>
EOF

  lifecycle {
    ignore_changes = [ values, tags ]
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
            "Sid": "DenyUnEncryptedObjectUploads",
            "Effect": "Deny",
            "Principal": "*",
            "Action": "s3:PutObject",
            "Resource": "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.arn}/*",
            "Condition": {
              "StringNotEquals": {
                "s3:x-amz-server-side-encryption": "aws:kms"
              }
            }
        },
        {
            "Sid": "JenkinsGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.arn}/*",
            "Condition": {
                "ArnEquals": {
                    "aws:userid": "arn:aws:iam::${local.account_id}:user/Jenkins"
                }
            }
        },
        {
            "Sid": "AllowSSLRequestsOnly",
            "Effect": "Deny",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": [
                "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.arn}",
                "arn:aws:s3:::${aws_s3_bucket.cloudfront_bucket.arn}/*"
            ],
            "Condition": {
                "Bool": {
                    "aws:SecureTransport": "false"
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

resource "aws_s3_bucket_policy" "cloudfront_logging" {
  bucket = aws_s3_bucket.cloudfront_logging.id
  policy = <<POLICY
{
  "Id": "LBAccessLogs",
  "Statement": [
    {
      "Action": "s3:PutObject",
      "Effect": "Allow",
      "Principal": {
        "AWS": "${local.aws_classic_loadbalancer_account_roots[local.region]}"
      },
      "Resource": "arn:aws:s3:::bfd-${local.env}-logs-${local.account_id}/*"
    },
    {
      "Action": "s3:*",
      "Condition": {
        "Bool": {
          "aws:SecureTransport": "false"
        }
      },
      "Effect": "Deny",
      "Principal": "*",
      "Resource": [
        "arn:aws:s3:::bfd-${local.env}-logs-${local.account_id}",
        "arn:aws:s3:::bfd-${local.env}-logs-${local.account_id}/*"
      ],
      "Sid": "AllowSSLRequestsOnly"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
}

resource "aws_cloudfront_distribution" "static_site_distribution" {
  origin {
    domain_name = aws_s3_bucket.cloudfront_bucket.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.cloudfront_bucket.id}"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.static_site_identity.cloudfront_access_identity_path
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "CloudFront distribution for static site ${local.env}"
  default_root_object = "index"

  aliases = [aws_route53_zone.static.name]

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
    Name = "StaticSite-CloudFront-${local.env}"
    Layer = local.layer
    role  = "static-site"
  }
}

resource "aws_cloudfront_origin_access_identity" "static_site_identity" {
  comment = "Origin access identity for static site ${local.env}"
}

