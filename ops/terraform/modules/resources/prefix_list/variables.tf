variable "tags" {
  description = "tags"
  type = map(string)
}

variable "name" {
  description = "(Required) Prefix list name (must not start with com.amazonaws)"
  type = string
}

variable "max_entries" {
  description = "(Required) Maximum number of entries this prefix list can support"
  type = number
}

variable "entries" {
  description = <<-EOD
(Optional) A map of entries in the form of {$cidr=$desc}.

Example:
    {"10.1.0.0/24"="foo network"},
    {"10.2.0.0/24"="bar network"}
EOD
  type = map(string)
  default = {}
}

variable "address_family" {
  description = "The address family (IPv4 or IPv6) of this list"
  type = string
  default = "IPv4"
}
