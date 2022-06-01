terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.12"
    }
    external = {
      source  = "hashicorp/external"
      version = "~> 2.2"
    }
  }
}
