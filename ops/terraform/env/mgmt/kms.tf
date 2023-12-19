locals {
  kms_default_deletion_window_days = 30
  kms_default_key_policy           = data.aws_iam_policy_document.default_kms_key_policy
}

# Define a default key policy doc that allows our root account and admins to manage and delegate access to the key. This
# policy statement must be included in every kms key policy, whether you use this doc or not. Without it, our account
# and account admins will not be able to manage the key. See the following for more info on default key policies:
#  - https://docs.aws.amazon.com/kms/latest/developerguide/key-policies.html#key-policy-default
data "aws_iam_policy_document" "default_kms_key_policy" {
  statement {
    sid    = "AllowKeyManagement"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${local.account_id}:root"]
    }
    actions   = ["kms:*"]
    resources = ["*"]
  }
}
