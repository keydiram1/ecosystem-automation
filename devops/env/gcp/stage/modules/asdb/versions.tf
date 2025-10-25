terraform {

  required_version = ">= 1.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "6.12.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "2.5.2"
    }
    time = {
      source  = "hashicorp/time"
      version = "0.12.1"
    }
    null = {
      source  = "hashicorp/null"
      version = "3.2.3"
    }
  }
}
