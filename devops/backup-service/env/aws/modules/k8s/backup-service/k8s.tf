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

resource "kubernetes_secret_v1" "tls_secret" {
  metadata {
    name      = "${local.service_name}-tls-secret"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "rootCA.pem" = base64decode(jsondecode(data.aws_secretsmanager_secret_version.tls.secret_string)["rootCA_pem"])
  }
}

resource "kubernetes_config_map_v1" "aerospike_backup_service_cm" {
  metadata {
    name      = "aerospike-backup-service-cm"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "aerospike-backup-service.yml" = yamlencode(local.remote_config)
  }
}

resource "kubernetes_secret_v1" "cluster_password" {
  metadata {
    name      = "psw-cm"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "psw.txt" = file(var.password_path)

  }
}

resource "kubernetes_service_account_v1" "backup_service_serviceaccount" {
  metadata {
    name        = local.service_name
    namespace   = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.backup_service_role.arn
    }
  }
}

resource "kubernetes_secret_v1" "backup_service_serviceaccount_secret" {
  metadata {
    name        = local.service_name
    namespace   = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "kubernetes.io/service-account.name" = kubernetes_service_account_v1.backup_service_serviceaccount.metadata.0.name
    }
  }
  type                           = "kubernetes.io/service-account-token"
  wait_for_service_account_token = true
}

resource "kubernetes_deployment_v1" "backup_service" {

  depends_on = [aws_iam_role_policy_attachment.backup_service_attachment, aws_s3_object.upload_remote_config]

  metadata {
    name      = "aerospike-backup-service"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    labels    = {
      app = "aerospike-backup-service"
    }
  }
  spec {
    replicas = "1"
    selector {
      match_labels = {
        app = "aerospike-backup-service"
      }
    }
    template {
      metadata {
        labels = {
          app = "aerospike-backup-service"
        }
      }
      spec {
        container {
          name  = "aerospike-backup-service"
          args  = ["-c", "/etc/aerospike-backup-service/aerospike-backup-service.yml", "-r"]
          #          command = ["/bin/sh", "-c"]
          #          args = ["while true; do echo 'Running...'; sleep 5; done"]
          image = "aerospike.jfrog.io/ecosystem-container-dev-local/aerospike-backup-service:${var.image_tag}"
          port {
            container_port = 8080
          }
          image_pull_policy = "Always"
          liveness_probe {
            http_get {
              path = "/health"
              port = 8080
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }
          readiness_probe {
            http_get {
              path = "/ready"
              port = 8080
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }
          volume_mount {
            mount_path = "/etc/aerospike-backup-service/aerospike-backup-service.yml"
            name       = "backup-config"
            sub_path   = "aerospike-backup-service.yml"
          }
          volume_mount {
            mount_path = "/config/psw.txt"
            name       = "psw"
            read_only  = true
            sub_path   = "psw.txt"
          }
          volume_mount {
            mount_path = "/etc/aerospike-backup-service/ssl/rootCA.pem"
            name       = "tls"
            read_only  = true
            sub_path   = "rootCA.pem"
          }
        }
        image_pull_secrets {
          name = kubernetes_secret_v1.regcred.metadata.0.name
        }
        service_account_name = kubernetes_service_account_v1.backup_service_serviceaccount.metadata.0.name
        volume {
          name = "backup-config"
          config_map {
            name     = kubernetes_config_map_v1.aerospike_backup_service_cm.metadata.0.name
            optional = true
          }
        }
        volume {
          name = "creds"
          secret {
            secret_name = kubernetes_secret_v1.regcred.metadata.0.name
            optional    = true
          }
        }
        volume {
          name = "psw"
          secret {
            secret_name = kubernetes_secret_v1.cluster_password.metadata.0.name
            optional    = true
          }
        }
        volume {
          name = "tls"
          secret {
            secret_name = kubernetes_secret_v1.tls_secret.metadata.0.name
            optional    = true
          }
        }
        affinity {
          pod_anti_affinity {
            required_during_scheduling_ignored_during_execution {
              topology_key = "kubernetes.io/hostname"
              label_selector {
                match_expressions {
                  key      = "app.kubernetes.io/name"
                  operator = "In"
                  values   = ["aerospike-backup-service"]
                }
              }
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "backup_service" {

  metadata {
    name      = "aerospike-backup-service"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    labels    = {
      app = kubernetes_deployment_v1.backup_service.metadata.0.name
    }
  }
  spec {
    selector = {
      app = kubernetes_deployment_v1.backup_service.metadata.0.name
    }
    port {
      port        = 8080
      protocol    = "TCP"
      target_port = 8080
    }
  }
}

resource "kubernetes_manifest" "virtual_service" {

  manifest = {
    apiVersion = "networking.istio.io/v1beta1"
    kind       = "VirtualService"
    metadata   = {
      name      = "abs-virtual-service"
      namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    }
    spec = {
      gateways = [
        "api",
      ]
      hosts = [
        data.kubernetes_service_v1.istio_gateway.status.0.load_balancer.0.ingress.0.hostname,
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
                host = kubernetes_service_v1.backup_service.metadata.0.name
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
