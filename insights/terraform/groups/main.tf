# Add to the IAM groups

data "aws_iam_group" "groups" {
  for_each    = var.insights_group_members
  group_name  = "bfd-insights-${each.key}"
}

resource "aws_iam_group_membership" "team" {
  for_each    = var.insights_group_members
  name        = "bfd-insights-${each.key}-team"
  group       = data.aws_iam_group.groups[each.key].group_name
  users       = each.value
}


# Create ad-hoc folders for analysts

data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "main" {
  bucket        = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_object" "user_folder" {
  for_each      = toset(var.insights_group_members["analysts"])
  bucket        = data.aws_s3_bucket.main.id
  content_type  = "application/x-directory"
  key           = "users/${each.value}/"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_object" "user_output_folder" {
  depends_on    = [aws_s3_bucket_object.user_folder]
  for_each      = toset(var.insights_group_members["analysts"])
  bucket        = data.aws_s3_bucket.main.id
  content_type  = "application/x-directory"
  key           = "users/${each.value}/query_results"

  lifecycle {
    prevent_destroy = true
  }
}





