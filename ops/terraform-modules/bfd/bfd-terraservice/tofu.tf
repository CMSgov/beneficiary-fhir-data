terraform {
  required_providers {
    external = {
      source  = "hashicorp/external"
      version = "~>2"
    }
    http = {
      source  = "hashicorp/http"
      version = "~>3"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "~>5"
    }
  }
  required_version = "~> 1.10.0"
}
