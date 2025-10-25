resource "helm_release" "backup_service" {
  name             = "abs"
  repository       = "https://aerospike.github.io/helm-charts"
  chart            = "aerospike-backup-service"
  namespace        = data.kubernetes_namespace_v1.namespace.metadata.0.name
  create_namespace = true
  wait             = true
  version          = "2.0.1"

  set {
    name  = "configmap.create"
    value = "false"
  }

  set {
    name  = "namespace"
    value = data.kubernetes_namespace_v1.namespace.metadata.0.name
  }

  set {
    name  = "image.repository"
    value = "aerospike.jfrog.io/ecosystem-container-dev-local/aerospike-backup-service"
  }

  set {
    name  = "image.tag"
    value = "${var.image_tag}"
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
    name = "args"
    value = ["-c", "/etc/aerospike-backup-service/aerospike-backup-service.yml"]
  }

  dynamic "set" {
    for_each = var.storage == "azure" ? [1] : []
    content {
      name  = "envFrom[0].secretRef.name"
      value = kubernetes_secret_v1.azure_credentials.0.metadata.0.name
    }
  }

  values = [
    yamlencode(local.config),
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
              labelSelector = {
                matchExpressions = [
                  {
                    key      = "app.kubernetes.io/name"
                    operator = "In"
                    values = ["aerospike-backup-service"]
                  }
                ]
              }
            }
          ]
        }
      }
    })
  ]
}
