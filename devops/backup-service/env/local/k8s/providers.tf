provider "aws" {
  region = "eu-central-1"
}

provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "kind-test-cluster"
}