data "aws_iam_policy_document" "assume_role" {
  for_each = toset(["ec2"])
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["${each.value}.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "kms" {
  statement {
    sid = "AllowEnvCMKAccess"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
      "kms:DescribeKey",
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "kms" {
  name        = "${local.name_prefix}-kms-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.name_prefix} EC2 Instance to use the ${local.env} CMK"
  policy      = data.aws_iam_policy_document.kms.json
}

data "aws_iam_policy_document" "ssm_params" {
  statement {
    sid = "AllowGetPipelineDBCredentials"
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = [
      for path in [
        "/bfd/${local.env}/database/sensitive/rds_master_password",
      ]
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(path, "/")}"
    ]
  }
}

resource "aws_iam_policy" "ssm_params" {
  name        = "${local.name_prefix}-ssm-params-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.name_prefix} EC2 Instance to get required SSM parameters"
  policy      = data.aws_iam_policy_document.ssm_params.json
}

resource "aws_iam_role" "this" {
  name                  = "${local.name_prefix}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.name_prefix} EC2 Instance"
  assume_role_policy    = data.aws_iam_policy_document.assume_role["ec2"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "this" {
  for_each = {
    kms        = aws_iam_policy.kms.arn
    ssm_params = aws_iam_policy.ssm_params.arn
  }

  role       = aws_iam_role.this.name
  policy_arn = each.value
}

resource "aws_iam_instance_profile" "this" {
  name = "${local.name_prefix}-instance-profile"
  role = aws_iam_role.this.name
}
