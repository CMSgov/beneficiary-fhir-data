// Managed prefix lists allows us to store and safely reference IP addresses/subnets in security groups or
// in other terraform resources without needing to hard-code the subnet.
//
// See https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ec2_managed_prefix_list
// for help on referencing an existing prefix list.

// Provision the prefix list (without entries).
resource "aws_ec2_managed_prefix_list" "prefix_list" {
  name           = var.name
  address_family = var.address_family
  max_entries    = var.max_entries
  tags           = var.tags
  dynamic "entry" {
    for_each = var.entries
    content {
      cidr = entry.key
      description = entry.value
    }
  }
}
