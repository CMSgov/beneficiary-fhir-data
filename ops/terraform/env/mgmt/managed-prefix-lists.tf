##
# Globally managed prefix lists that we can reference in terraform, security groups, nacl's, etc.
##

# Cloudbees core jenkins subnet.
resource "aws_ec2_managed_prefix_list" "prefix_list" {
  name           = "bfd-cbc-jenkins"
  address_family = "IPv4"
  max_entries    = 1
  dynamic "entry" {
    for_each = var.cbc_jenkins_prefix_list
    content {
      cidr        = entry.key
      description = entry.value
    }
  }
}

variable "cbc_jenkins_prefix_list" {
  description = "prefix list entrie(s) for cbc core jenkins"
  type        = map(string)
  default     = {}
}
