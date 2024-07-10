resource "aws_cloudfront_distribution" "static_site_distribution" {
  depends_on = [aws_cloudfront_origin_access_identity.static_site_identity, aws_s3_bucket.static_site, aws_s3_bucket.cloudfront_logging]
  origin {
    domain_name = aws_s3_bucket.static_site.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.static_site.bucket}"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.static_site_identity.cloudfront_access_identity_path
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "CloudFront distribution for static site ${local.env}"
  default_root_object = "/index.html"

  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/error.html"
  }

  logging_config {
    include_cookies = false
    bucket          = aws_s3_bucket.cloudfront_logging.bucket_domain_name ## local.static_logging_bucket_ref 
    prefix          = "${local.env}-static-logs/"
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${aws_s3_bucket.static_site.bucket}"
    viewer_protocol_policy = "redirect-to-https"

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 0
  }

  restrictions {
    geo_restriction {
      restriction_type = "whitelist"
      locations        = ["US"]
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
    ## TODO: BFD-3489
    # acm_certificate_arn      = data.aws_acm_certificate.env_issued.arn
    # minimum_protocol_version = "TLSv1.2_2021"
    # ssl_support_method       = "sni-only"
  }

  tags = {
    Name  = "StaticSite-CloudFront-${local.env}"
    Layer = local.layer
    role  = "static_site"
  }
}

resource "aws_cloudfront_origin_access_identity" "static_site_identity" {
  comment = "Origin access identity for static site ${local.env}"
}
