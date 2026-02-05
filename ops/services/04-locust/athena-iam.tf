data "aws_iam_policy_document" "crawler_s3" {
  statement {
    sid = "AllowBucketAccess"
    actions = [
      "s3:ListBucketMultipartUploads",
      "s3:ListBucket",
      "s3:HeadBucket",
      "s3:GetBucketLocation"
    ]
    resources = [module.bucket_athena.bucket.arn]
  }

  statement {
    sid = "AllowBucketObjectAccess"
    actions = [
      "s3:PutObject*",
      "s3:ListMultipartUploadParts",
      "s3:GetObject*",
      "s3:DeleteObject*",
      "s3:AbortMultipartUpload"
    ]
    resources = ["${module.bucket_athena.bucket.arn}/*"]
  }
}

resource "aws_iam_policy" "crawler_s3" {
  name = "${local.locust_stats_crawler_name}-s3"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.locust_stats_crawler_name} Glue Crawler to use the ",
    "${module.bucket_athena.bucket.bucket} S3 Bucket"
  ])
  policy = data.aws_iam_policy_document.crawler_s3.json
}

data "aws_iam_policy_document" "crawler_kms" {
  statement {
    sid = "AllowEnvCMKUsage"
    actions = [
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:Encrypt",
      "kms:DescribeKey",
      "kms:Decrypt"
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "crawler_kms" {
  name = "${local.locust_stats_crawler_name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.locust_stats_crawler_name} Glue Crawler to use the ",
    "${local.env_key_alias} CMK"
  ])
  policy = data.aws_iam_policy_document.crawler_kms.json
}

data "aws_iam_policy" "amazon_athena_full_access" {
  name = "AmazonAthenaFullAccess"
}

data "aws_iam_policy" "aws_glue_service_role" {
  name = "AWSGlueServiceRole"
}

data "aws_iam_policy_document" "glue_assume_crawler" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["glue.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "crawler" {
  name                  = "${local.locust_stats_crawler_name}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.locust_stats_crawler_name} Glue Crawler"
  assume_role_policy    = data.aws_iam_policy_document.glue_assume_crawler.json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "crawler" {
  for_each = {
    s3           = aws_iam_policy.crawler_s3.arn
    kms          = aws_iam_policy.crawler_kms.arn
    athena_full  = data.aws_iam_policy.amazon_athena_full_access.arn
    glue_service = data.aws_iam_policy.aws_glue_service_role.arn
  }

  role       = aws_iam_role.crawler.name
  policy_arn = each.value
}
