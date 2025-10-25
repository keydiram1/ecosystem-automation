resource "kubernetes_secret_v1" "regcred" {
  metadata {
    name      = "regcred"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }
  data = {
    ".dockerconfigjson" = jsonencode({
      auths = {
        "aerospike.jfrog.io" = {
          "username" = data.google_secret_manager_secret_version_access.docker_username.secret_data
          "password" = data.google_secret_manager_secret_version_access.docker_password.secret_data
        }
      }
    })
  }
  type = "kubernetes.io/dockerconfigjson"
}

resource "kubernetes_secret_v1" "tls_secret" {
  metadata {
    name      = "${local.module_name}-tls-secret"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "ca.aerospike.com.pem" = data.google_secret_manager_secret_version_access.ca_aerospike_com_pem.secret_data
  }
}

resource "kubernetes_secret_v1" "azure_credentials" {
  count = var.storage == "azure" ? 1 : 0
  metadata {
    name      = "credentials"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "AZURE_TENANT_ID" = data.google_secret_manager_secret_version_access.azure_tenant_id.0.secret_data
    "AZURE_CLIENT_ID" = data.google_secret_manager_secret_version_access.azure_client_id.0.secret_data
    "AZURE_CLIENT_SECRET" = data.google_secret_manager_secret_version_access.azure_client_secret.0.secret_data
  }
}

resource "kubernetes_secret_v1" "s3_credentials" {
  count = var.storage == "s3" ? 1 : 0
  metadata {
    name      = "credentials"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "credentials" = <<-EOF
    [s3]
    aws_access_key_id="${data.google_secret_manager_secret_version_access.aws_access_key_id.0.secret_data}"
    aws_secret_access_key="${data.google_secret_manager_secret_version_access.aws_secret_access_key.0.secret_data}"
    EOF
  }
}

resource "kubernetes_secret_v1" "minio_credentials" {
  count = var.storage == "minio" ? 1 : 0
  metadata {
    name      = "credentials"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "credentials" = <<-EOF
    [minio]
    aws_access_key_id=minioadmin
    aws_secret_access_key=minioadmin
    EOF
  }
}

resource "kubernetes_service_account_v1" "abs_sa" {
  metadata {
    name      = local.service_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "iam.gke.io/gcp-service-account" = data.google_service_account.abs.email
    }
  }
}

resource "kubernetes_secret_v1" "backup_service_serviceaccount_secret" {
  metadata {
    name      = local.service_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "kubernetes.io/service-account.name" = kubernetes_service_account_v1.abs_sa.metadata.0.name
    }
  }
  type                           = "kubernetes.io/service-account-token"
  wait_for_service_account_token = true
}

resource "kubernetes_manifest" "virtual_service" {

  manifest = {
    apiVersion = "networking.istio.io/v1beta1"
    kind       = "VirtualService"
    metadata = {
      name      = "abs-virtual-service"
      namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    }
    spec = {
      gateways = [
        "api",
      ]
      hosts = [
        data.kubernetes_service_v1.istio_gateway.status.0.load_balancer.0.ingress.0.ip,
      ]
      http = [
        {
          match = [
            {
              uri = {
                prefix = "/"
              }
              port = 80
            },
          ]
          route = [
            {
              destination = {
                host = data.kubernetes_service_v1.abs_svc.metadata.0.name
                port = {
                  number = 8080
                }
              }
            },
          ]
        },
      ]
    }
  }
}
