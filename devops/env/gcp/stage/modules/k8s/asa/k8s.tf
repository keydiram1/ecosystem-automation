resource "kubernetes_service_account_v1" "secret_agent_sa" {
  metadata {
    name      = local.service_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "iam.gke.io/gcp-service-account" = google_service_account.asa.email
    }
  }
}

resource "kubernetes_secret_v1" "secret_agent_sa_secret" {
  metadata {
    name      = local.service_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "kubernetes.io/service-account.name" = kubernetes_service_account_v1.secret_agent_sa.metadata.0.name
    }
  }
  type                           = "kubernetes.io/service-account-token"
  wait_for_service_account_token = true
}

resource "kubernetes_config_map_v1" "secret_agent_cm" {
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
        gcp = {
          convert-to-base64 = true
          resources = {
            caCert             = data.google_secret_manager_secret_version.ca-aerospike-com-pem.id
            asdKey             = data.google_secret_manager_secret_version.asd-aerospike-com-key.id
            asdCert            = data.google_secret_manager_secret_version.asd-aerospike-com-pem.id
            encKey             = data.google_secret_manager_secret_version.encryption-key-pem.id
            asdPsw             = data.google_secret_manager_secret_version.password-txt.id
            awsAccessKey       = data.google_secret_manager_secret_version.aws-access-key-id.id
            awsSecretAccessKey = data.google_secret_manager_secret_version.aws-secret-access-key.id
            azureTenantID      = data.google_secret_manager_secret_version.azure-tenant-id.id
            azureClientID      = data.google_secret_manager_secret_version.azure-client-id.id
            azureClientSecret  = data.google_secret_manager_secret_version.azure-client-secret.id
            gcpSaKeyFile       = data.google_secret_manager_secret_version.gcp-sa-key-file.id
            featuresConf = data.google_secret_manager_secret_version.features-conf.id
          }
        }
      }
    })
  }
}

resource "kubernetes_deployment_v1" "secret_agent" {

  depends_on = [
    google_service_account.asa,
    google_project_iam_member.secret_accessor,
    google_project_iam_member.workload_identity_user
  ]

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
          name  = local.service_name
          args = ["--config-file", "/etc/secretagent/config.yaml"]
          #          command = ["/bin/sh", "-c"]
          #          args = ["while true; do echo 'Running...'; sleep 5; done"]
          image = "aerospike/aerospike-secret-agent:${var.image_tag}"
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
            read_only  = true
          }
          #           volume_mount {
          #             mount_path = "/etc/secretagent/ssl"
          #             name       =  "${local.service_name}-tls"
          #             read_only = true
          #           }
        }
        service_account_name = kubernetes_service_account_v1.secret_agent_sa.metadata.0.name
        volume {
          name = "${local.service_name}-config"
          config_map {
            name     = kubernetes_config_map_v1.secret_agent_cm.metadata.0.name
            optional = true
          }
        }
        #         volume {
        #           name = "${local.service_name}-tls"
        #           secret {
        #             secret_name = kubernetes_secret_v1.tls_secret.metadata.0.name
        #             optional = true
        #           }
        #         }

        affinity {
          pod_anti_affinity {
            required_during_scheduling_ignored_during_execution {
              topology_key = "kubernetes.io/hostname"
              label_selector {
                match_expressions {
                  key      = "app.kubernetes.io/name"
                  operator = "In"
                  values = [local.service_name]
                }
              }
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "secret_agent_svc" {

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
      name        = "secret-agent"
      port        = 3005
      protocol    = "TCP"
      target_port = 3005
    }
    port {
      name        = "metrics"
      port        = 8080
      protocol    = "TCP"
      target_port = 8080
    }
  }
}

resource "kubernetes_manifest" "vs" {

  manifest = {
    apiVersion = "networking.istio.io/v1beta1"
    kind       = "VirtualService"
    metadata = {
      name      = "${local.service_name}-vs"
      namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    }
    spec = {
      gateways = [
        "api",
      ]
      hosts = [
        data.kubernetes_service_v1.istio_gateway.status.0.load_balancer.0.ingress.0.ip,
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
                host = kubernetes_service_v1.secret_agent_svc.metadata.0.name
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
