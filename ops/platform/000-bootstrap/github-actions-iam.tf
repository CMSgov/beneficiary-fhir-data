# TODO: Define custom policies restricting access to only necessary permissions
data "aws_iam_policy" "power_user_access" {
  name = "PowerUserAccess"
}

data "aws_iam_policy_document" "github_actions_iam" {
  statement {
    sid = "AllowRoleAndPolicyModifications"
    actions = [
      "iam:Describe*Role*",
      "iam:Describe*Polic*",
      "iam:List*Role*",
      "iam:List*Polic*",
      "iam:AttachRolePolicy",
      "iam:CreatePolicy*",
      "iam:CreateRole",
      "iam:DeletePolicy*",
      "iam:DeleteRole*",
      "iam:DetachRolePolicy",
      "iam:GetPolicy*",
      "iam:GetRole*",
      "iam:PutRolePolicy",
      "iam:PutRolePermissionsBoundary",
      "iam:TagRole*",
      "iam:TagPolicy*",
      "iam:UntagRole*",
      "iam:UntagPolicy*",
      "iam:AssumeRolePolicy*",
      "iam:UpdateRole*",
      "iam:PassRole",
      "iam:ListAccountAliases",
      "iam:*OpenID*"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "github_actions_iam" {
  name        = "bfd-${local.env}-github-actions-iam-policy"
  path        = local.iam_path
  description = "Grants permissions for GitHub Actions Runners to make modifications to IAM Roles and Policies"
  policy      = data.aws_iam_policy_document.github_actions_iam.json
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  client_id_list = ["sts.amazonaws.com"]
  url            = "https://token.actions.githubusercontent.com"
}

data "aws_iam_policy_document" "github_actions_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    condition {
      test     = "ForAllValues:StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:CMSgov/beneficiary-fhir-data:*"]
    }

    condition {
      test     = "ForAllValues:StringLike"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }
  }
}

resource "aws_iam_role" "github_actions" {
  name        = "bfd-${local.env}-github-actions"
  path        = local.iam_path
  description = "OIDC Assumable GitHub Actions Role"

  permissions_boundary = local.permissions_boundary

  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json

  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "github_actions" {
  for_each = {
    power_user_access = data.aws_iam_policy.power_user_access.arn
    iam               = aws_iam_policy.github_actions_iam.arn
  }

  role       = aws_iam_role.github_actions.name
  policy_arn = each.value
}
