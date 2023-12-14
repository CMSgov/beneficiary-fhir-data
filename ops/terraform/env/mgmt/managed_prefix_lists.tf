##
# Globally managed prefix lists that we can reference in terraform, security groups, nacl's, etc.
##

# Cloudbees core jenkins subnet.
module "cbc_jenkins" {
  source         = "../../modules/resources/prefix_list"
  name           = "bfd-cbc-jenkins"
  max_entries    = 1
  entries        = var.cbc_jenkins_prefix_list
  address_family = "IPv4"
}

variable "cbc_jenkins_prefix_list" {
  description = "prefix list entrie(s) for cbc core jenkins"
  type        = map(string)
  default     = {}
}
