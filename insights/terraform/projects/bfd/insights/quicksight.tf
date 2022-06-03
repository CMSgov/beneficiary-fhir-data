# QuickSight resources we'd like to import:
#
# data_set          Cannot currently be imported (see https://github.com/hashicorp/terraform-provider-aws/issues/10990)
# dashboard         Cannot currently be imported
#
# What about the analysis?

# resource "aws_quicksight_data_source" "bfd-beneficiaries" {
#   for_each = local.environments

#   name    = "bfd-${each.key}-beneficiaries"
#   data_source_id = "bfd-${each.key}-beneficiaries"
#   type    = "ATHENA"
#   parameters {
#     athena {
#       work_group = "bfd"
#     }
#   }
# }

# TODO: Add a QuickSight user based on IAM user
