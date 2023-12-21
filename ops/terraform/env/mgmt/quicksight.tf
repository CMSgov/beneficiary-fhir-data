locals {
  quicksight_config = zipmap(
    [for name in data.aws_ssm_parameters_by_path.sensitive_quicksight_config.names :
    element(split("/", name), length(split("/", name)) - 1)],
    [for value in nonsensitive(data.aws_ssm_parameters_by_path.sensitive_quicksight_config.values) : value]
  )

  quicksight_users  = jsondecode(local.quicksight_config["users"])
  quicksight_groups = [for group in jsondecode(local.quicksight_config["groups"]) : group["group_name"]]
  quicksight_group_memberships = flatten([
    for user in local.quicksight_users : [
      for group_name in user["groups"] : { "member_name" : lookup(user, "iam", user["email"]), "group_name" : group_name }
    ]
  ])

  quicksight_principal_admin_arn          = local.quicksight_config["principal_admin_arn"]
  transfer_quicksight_permissions_script  = "${path.module}/scripts/transfer-quicksight-permissions.sh"
  transfer_quicksight_permissions_command = <<-EOF
chmod +x ${local.transfer_quicksight_permissions_script}
${local.transfer_quicksight_permissions_script}
EOF
}

data "aws_ssm_parameters_by_path" "sensitive_quicksight_config" {
  path = "/bfd/mgmt/quicksight/sensitive/"
}

# Quicksight users are expected to adhere to the following json format:
# {"email":"", "user_role":"AUTHOR|ADMIN|READER", "identity_type":"QUICKSIGHT|IAM","groups":[""]}
# If "IAM" is specified as the identity_type, then "iam_arn" must be provided
# If "QUICKSIGHT" is specified as the identity_type, then "user_name" must be provided
resource "aws_quicksight_user" "quicksight_user" {
  for_each = { for idx, user in local.quicksight_users : idx => user }

  email         = sensitive(each.value["email"])
  identity_type = each.value["identity_type"]
  iam_arn       = each.value["identity_type"] == "IAM" ? "arn:aws:iam::${local.account_id}:user/${each.value["iam"]}" : null
  user_role     = upper(each.value["user_role"])
  user_name     = each.value["identity_type"] == "QUICKSIGHT" ? sensitive(each.value["email"]) : each.value["iam"]
}

# On deletion, transfer ownership of assets (for which this user is the sole owner) to the principal admin as defined
# in SSM
resource "null_resource" "destroy_quicksight_user" {
  for_each = aws_quicksight_user.quicksight_user
  triggers = {
    account_id          = local.account_id
    sole_owner_arn      = each.value.arn,
    principal_admin_arn = local.quicksight_principal_admin_arn
    command             = local.transfer_quicksight_permissions_command
  }

  provisioner "local-exec" {
    when    = destroy
    command = self.triggers.command
    environment = {
      AWS_ACCOUNT_ID      = self.triggers.account_id
      PRINCIPAL_ADMIN_ARN = self.triggers.principal_admin_arn
      SOLE_OWNER_ARN      = self.triggers.sole_owner_arn
    }
  }
}

# Quicksight groups are expected to adhere to the following json format:
# {"group_name":""}
resource "aws_quicksight_group" "quicksight_group" {
  for_each = toset(local.quicksight_groups)

  group_name = each.key
}

# Quicksight group memberships are specified as part of the quicksight users json object's "groups" attribute
# For each user, for each group, we add an entry to a map and then create that group membership iteratively
# e.g., { "0" => "user_name": "group_name", "1" => "user_name": "group_name" }
resource "aws_quicksight_group_membership" "quicksight_group_membership" {
  for_each = { for idx, group_membership in local.quicksight_group_memberships : idx => group_membership }

  group_name  = each.value["group_name"]
  member_name = sensitive(each.value["member_name"])
  depends_on  = [aws_quicksight_group.quicksight_group, aws_quicksight_user.quicksight_user]
}
