terraform {

  required_version = ">= 1.0"

  required_providers {
    http = {
      source  = "hashicorp/http"
      version = "3.4.5"
    }
    local = {
      source  = "hashicorp/local"
      version = "2.5.2"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "2.16.1"
    }
  }
}
