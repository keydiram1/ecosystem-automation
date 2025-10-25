resource "helm_release" "backup_service" {
  name = "abs"
  chart = (
    var.helm_chart_source == "repo"
    ? "${path.root}/aerospike-backup-service/helm"
    : "aerospike-backup-service"
  )
  repository = (
    var.helm_chart_source == "repo"
    ? null
    : "https://aerospike.jfrog.io/artifactory/api/helm/ecosystem-helm-prod-local"
  )
  namespace        = kubernetes_namespace_v1.abs_namespace.metadata.0.name
  create_namespace = true
  wait             = true
  version          = var.helm_chart_source == "repo" ? null : "2.0.4"

  depends_on = [null_resource.abs]

  repository_username = data.google_secret_manager_secret_version_access.jfrog_username.secret_data
  repository_password = data.google_secret_manager_secret_version_access.jfrog_password.secret_data

  set {
    name  = "configmap.create"
    value = "false"
  }

  set {
    name  = "namespace"
    value = kubernetes_namespace_v1.abs_namespace.metadata.0.name
  }

  set {
    name  = "image.repository"
    value = "aerospike.jfrog.io/ecosystem-container-dev-local/aerospike-backup-service"
  }

  set {
    name  = "image.tag"
    value = var.image_tag
  }

  set {
    name  = "serviceAccount.create"
    value = "false"
  }

  set {
    name  = "serviceAccount.name"
    value = kubernetes_service_account_v1.abs_sa.metadata.0.name
  }

  set_list {
    name  = "args"
    value = ["-c", "/etc/aerospike-backup-service/aerospike-backup-service.yml"]
  }

  dynamic "set" {
    for_each = var.storage_provider == "azure" ? [1] : []
    content {
      name  = "envFrom[0].secretRef.name"
      value = kubernetes_secret_v1.azure_credentials.0.metadata.0.name
    }
  }

  values = [
    yamlencode({
      podSecurityContext = {
        runAsUser           = 65532
        runAsGroup          = 65532
        fsGroup             = 65532
        fsGroupChangePolicy = "Always"
      }
    }),
    data.local_file.abs_configuration.content,
    yamlencode({
      extraSecretVolumeMounts = local.finalSecretVolumeMounts
    }),
    yamlencode({
      imagePullSecrets = [
        {
          name = kubernetes_secret_v1.regcred.metadata.0.name
        }
      ]
    }),
    yamlencode({
      volumes = local.finalVolumes
    }),
    yamlencode({
      affinity = {
        podAntiAffinity = {
          requiredDuringSchedulingIgnoredDuringExecution = [
            {
              topologyKey = "kubernetes.io/hostname"
              namespaces  = ["ws-1", "ws-2", "ws-3"]
              labelSelector = {
                matchExpressions = [
                  {
                    key      = "app.kubernetes.io/name"
                    operator = "In"
                    values   = ["aerospike-backup-service"]
                  }
                ]
              }
            }
          ]
        }
        nodeAffinity = {
          requiredDuringSchedulingIgnoredDuringExecution = {
            nodeSelectorTerms = [
              {
                matchExpressions = [
                  {
                    key      = "cloud.google.com/gke-nodepool"
                    operator = "In"
                    values   = ["aerospike-backup-service-pool"]
                  }
                ]
              }
            ]
          }
        }
      }
    })
  ]
}
