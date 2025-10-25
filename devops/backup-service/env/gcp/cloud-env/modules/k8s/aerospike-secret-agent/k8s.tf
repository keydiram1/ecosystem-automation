resource "kubernetes_service_account_v1" "aerospike_secret_agent" {
  metadata {
    name      = local.module_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "iam.gke.io/gcp-service-account" = google_service_account.aerospike_secret_agent.email
    }
  }
}

resource "kubernetes_secret_v1" "aerospike_secret_agent" {
  metadata {
    name      = local.module_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    annotations = {
      "kubernetes.io/service-account.name" = kubernetes_service_account_v1.aerospike_secret_agent.metadata.0.name
    }
  }
  type                           = "kubernetes.io/service-account-token"
  wait_for_service_account_token = true
}

resource "kubernetes_config_map_v1" "aerospike_secret_agent" {
  metadata {
    name      = local.module_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  data = {
    "aerospike-secret-agent.yaml" = yamlencode({
      service = {
        http = {
          endpoint = "0.0.0.0:${local.manage_port}"
        }
        tcp = {
          endpoint = "0.0.0.0:${local.service_port}"
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
            caCert             = data.google_secret_manager_secret_version.ca_aerospike_com_pem.id
            asdKey             = data.google_secret_manager_secret_version.asd_aerospike_com_key.id
            asdCert            = data.google_secret_manager_secret_version.asd_aerospike_com_pem.id
            encKey             = data.google_secret_manager_secret_version.encryption_key_pem.id
            asdPsw             = data.google_secret_manager_secret_version.password_txt.id
            awsAccessKey       = data.google_secret_manager_secret_version.aws_access_key_id.id
            awsSecretAccessKey = data.google_secret_manager_secret_version.aws_secret_access_key.id
            azureTenantID      = data.google_secret_manager_secret_version.azure_tenant_id.id
            azureClientID      = data.google_secret_manager_secret_version.azure_client_id.id
            azureClientSecret  = data.google_secret_manager_secret_version.azure_client_secret.id
            gcpSaKeyFile       = data.google_secret_manager_secret_version.gcp_sa_key_file.id
            featuresConf       = data.google_secret_manager_secret_version.features_conf.id
          }
        }
      }
    })
  }
}

resource "kubernetes_deployment_v1" "aerospike_secret_agent" {

  depends_on = [
    google_service_account.aerospike_secret_agent,
    google_project_iam_member.aerospike_secret_agent_secret_accessor,
    google_project_iam_member.aerospike_secret_agent_workload_identity
  ]

  metadata {
    name      = local.module_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    labels = {
      app = local.module_name
    }
  }

  spec {
    replicas = "1"

    selector {
      match_labels = {
        app = local.module_name
      }
    }

    template {
      metadata {
        labels = {
          app = local.module_name
        }
      }

      spec {
        container {
          name  = local.module_name
          args = ["--config-file", "${local.filepath}/${local.filename}"]
          image = "aerospike/${local.module_name}:${local.latest_version}"

          port {
            container_port = local.service_port
          }

          port {
            container_port = local.manage_port
          }

          liveness_probe {
            http_get {
              path = "/manage"
              port = local.manage_port
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }

          readiness_probe {
            http_get {
              path = "/manage"
              port = local.manage_port
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }

          volume_mount {
            mount_path = "${local.filepath}/${local.filename}"
            name       = local.module_name
            sub_path   = local.filename
            read_only  = true
          }

          # volume_mount {
          #   mount_path = "/etc/secretagent/ssl"
          #   name       = "${local.service_name}-tls"
          #   read_only  = true
          # }
        }

        service_account_name = kubernetes_service_account_v1.aerospike_secret_agent.metadata.0.name

        volume {
          name = local.module_name
          config_map {
            name     = kubernetes_config_map_v1.aerospike_secret_agent.metadata.0.name
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
                  values = [local.module_name]
                }
              }
            }
          }
          node_affinity {
            required_during_scheduling_ignored_during_execution {
              node_selector_term {
                match_expressions {
                  key      = "cloud.google.com/gke-nodepool"
                  operator = "In"
                  values = ["${local.module_name}-pool"]
                }
              }
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "aerospike_secret_agent" {

  metadata {
    name      = local.module_name
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    labels = {
      app = kubernetes_deployment_v1.aerospike_secret_agent.metadata.0.name
    }
  }
  spec {
    selector = {
      app = kubernetes_deployment_v1.aerospike_secret_agent.metadata.0.name
    }
    port {
      name        = "secret-agent"
      port        = local.service_port
      protocol    = "TCP"
      target_port = local.service_port
    }
    port {
      name        = "metrics"
      port        = local.manage_port
      protocol    = "TCP"
      target_port = local.manage_port
    }
  }
}

resource "kubernetes_manifest" "aerospike_secret_agent" {

  manifest = {
    apiVersion = "networking.istio.io/v1beta1"
    kind       = "VirtualService"
    metadata = {
      name      = local.module_name
      namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    }
    spec = {
      gateways = [
        "api",
      ]
      hosts = [
        data.kubernetes_service_v1.gateway.status.0.load_balancer.0.ingress.0.ip,
        "gateway.ecosys.internal",
      ]
      tcp = [
        {
          match = [
            {
              port = local.service_port
            },
          ]
          route = [
            {
              destination = {
                host = kubernetes_service_v1.aerospike_secret_agent.metadata.0.name
                port = {
                  number = local.service_port
                }
              }
            },
          ]
        },
      ]
    }
  }
}
