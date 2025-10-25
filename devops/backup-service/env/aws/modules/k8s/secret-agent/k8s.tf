resource "kubernetes_service_account_v1" "backup_service_serviceaccount" {
  metadata {
    name      = local.service_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.secret_agent_role.arn
    }
  }
}

resource "kubernetes_secret_v1" "backup_service_serviceaccount_secret" {
  metadata {
    name      = local.service_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "kubernetes.io/service-account.name" = kubernetes_service_account_v1.backup_service_serviceaccount.metadata.0.name
    }
  }
  type                           = "kubernetes.io/service-account-token"
  wait_for_service_account_token = true
}

resource "kubernetes_config_map_v1" "aerospike_secret_agent_cm" {
  metadata {
    name      = "${local.service_name}-cm"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "config.yaml" = yamlencode({
      service = {
        http = {
          endpoint = "0.0.0.0:8080"
        }
        tcp = {
          endpoint = "0.0.0.0:3005"
#          tls = {
#            cert-file = "/etc/secretagent/ssl/tls.pem"
#            key-file = "/etc/secretagent/ssl/tls.key"
#          }
        }
      }
      secret-manager = {
        aws = {
          region = data.aws_region.current.name
          convert-to-base64 = false
          resources = {
            TestEnvTls = data.aws_secretsmanager_secret.tls.arn
          }
        }
      }
    })
  }
}

resource "kubernetes_deployment_v1" "secret_agent" {

#  depends_on = [aws_iam_role.backup_service_role, aws_iam_policy.backup_service_policy, aws_iam_role_policy_attachment.backup_service_attachment]
  depends_on = [aws_iam_role.secret_agent_role, aws_iam_role_policy_attachment.secret-agent_attachment]

  metadata {
    name      = local.service_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    labels = {
      app = local.service_name
    }
  }
  spec {
    replicas = "1"
    selector {
      match_labels = {
        app = local.service_name
      }
    }
    template {
      metadata {
        labels = {
          app = local.service_name
        }
      }
      spec {
        container {
          name = local.service_name
          args = ["--config-file", "/etc/secretagent/config.yaml"]
#          command = ["/bin/sh", "-c"]
#          args = ["while true; do echo 'Running...'; sleep 5; done"]
          image   = "aerospike/aerospike-secret-agent:${var.image_tag}"
          port {
            container_port = 3005
          }
          port {
            container_port = 8080
          }
          liveness_probe {
            http_get {
              path = "/manage"
              port = 8080
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }
          readiness_probe {
            http_get {
              path = "/manage"
              port = 8080
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }
          volume_mount {
            mount_path = "/etc/secretagent/config.yaml"
            name       = "${local.service_name}-config"
            sub_path   = "config.yaml"
            read_only = true
          }
          volume_mount {
            mount_path = "/etc/secretagent/ssl"
            name       =  "${local.service_name}-tls"
            read_only = true
          }
        }
        service_account_name = kubernetes_service_account_v1.backup_service_serviceaccount.metadata.0.name
        volume {
          name = "${local.service_name}-config"
          config_map {
            name     = kubernetes_config_map_v1.aerospike_secret_agent_cm.metadata.0.name
            optional = true
          }
        }
        volume {
          name = "${local.service_name}-tls"
          secret {
            secret_name = kubernetes_secret_v1.tls_secret.metadata.0.name
            optional = true
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
                  values   = [local.service_name]
                }
              }
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "secret_agent_service" {

  metadata {
    name      = local.service_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    labels = {
      app = kubernetes_deployment_v1.secret_agent.metadata.0.name
    }
  }
  spec {
    selector = {
      app = kubernetes_deployment_v1.secret_agent.metadata.0.name
    }
    port {
      name = "secret-manager"
      port        = 3005
      protocol    = "TCP"
      target_port = 3005
    }
    port {
      name = "metrics"
      port        = 8080
      protocol    = "TCP"
      target_port = 8080
    }
  }
}

resource "kubernetes_secret_v1" "tls_secret" {
  metadata {
    name = "${local.service_name}-tls-secret"
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "tls.pem" = base64decode(jsondecode(data.aws_secretsmanager_secret_version.tls.secret_string)["agent_pem"])
    "tls.key" = base64decode(jsondecode(data.aws_secretsmanager_secret_version.tls.secret_string)["agent_key"])
  }
}

resource "kubernetes_manifest" "virtual_service" {

  manifest = {
    apiVersion = "networking.istio.io/v1beta1"
    kind = "VirtualService"
    metadata = {
      name = "secret-agent-virtual-service"
      namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    }
    spec = {
      gateways = [
        "api",
      ]
      hosts = [
        data.kubernetes_service_v1.istio_gateway.status.0.load_balancer.0.ingress.0.hostname,
      ]
      tcp = [
        {
          match = [
            {
              port = 3005
            },
          ]
          route = [
            {
              destination = {
                host = kubernetes_service_v1.secret_agent_service.metadata.0.name
                port = {
                  number = 3005
                }
              }
            },
          ]
        },
      ]
    }
  }
}
