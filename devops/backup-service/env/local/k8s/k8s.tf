resource "kubernetes_namespace_v1" "namespace" {
  metadata {
    name = "aerospike"
  }
}

resource "kubernetes_secret_v1" "regcred" {

  metadata {
    name      = "regcred"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    ".dockerconfigjson" = data.aws_secretsmanager_secret_version.regcred.secret_string
  }
  type = "kubernetes.io/dockerconfigjson"

}

resource "kubernetes_secret_v1" "credentials" {

  metadata {
    name      = "creds-cm"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "credentials" = file("../creds/credentials")
  }

}

resource "kubernetes_config_map_v1" "aerospike-backup-service-cm" {

  metadata {
    name      = "aerospike-backup-service-cm"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "backup-config.yaml" = file("../backup-config/config.yaml")
  }
}

resource "kubernetes_secret_v1" "cluster_password" {

  metadata {
    name      = "psw-cm"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "credentials" = file("../creds/psw.txt")
  }

}