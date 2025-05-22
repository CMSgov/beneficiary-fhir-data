# Define a default key policy doc that allows our root account and admins to manage and delegate
# access to the key. This policy statement must be included in every kms key policy, whether you use
# this doc or not. Without it, our account and account admins will not be able to manage the key.
# See the following for more info on default key policies:
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

resource "aws_kms_key" "tf_state" {
  for_each = toset(local.state_variants)

  description                        = "Terraform S3 state bucket key used to encrypt ${each.key} state bucket."
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
  deletion_window_in_days            = 30

  policy = data.aws_iam_policy_document.default_kms_key_policy.json

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Environment = each.key
    stack       = each.key
  }
}

# key aliases for data protection
resource "aws_kms_alias" "tf_state" {
  for_each = toset(local.state_variants)

  name          = "alias/bfd-${each.key}-tf-state"
  target_key_id = aws_kms_key.tf_state[each.key].arn
}
