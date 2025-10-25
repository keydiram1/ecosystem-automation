resource "helm_release" "istio_base" {
  name             = "base"
  repository       = "https://istio-release.storage.googleapis.com/charts"
  chart            = "base"
  namespace        = "istio-system"
  create_namespace = true
  version          = local.base_istio_version

  set {
    name  = "defaultRevision"
    value = "default"
  }
}

resource "helm_release" "istiod" {
  name = "istiod"

  repository       = "https://istio-release.storage.googleapis.com/charts"
  chart            = "istiod"
  namespace        = "istio-system"
  create_namespace = true
  version          = local.istiod_istio_version

  values = [
    yamlencode({
      global = {
        nodeSelector = {
          "cloud.google.com/gke-nodepool" = "main-pool"
        }
        affinity = {
          nodeAffinity = {
            requiredDuringSchedulingIgnoredDuringExecution = {
              nodeSelectorTerms = [
                {
                  matchExpressions = [
                    {
                      key      = "cloud.google.com/gke-nodepool"
                      operator = "In"
                      values = ["main-pool"]
                    }
                  ]
                }
              ]
            }
          }
          podAntiAffinity = {
            preferredDuringSchedulingIgnoredDuringExecution = [
              {
                weight = 100
                podAffinityTerm = {
                  labelSelector = {
                    matchExpressions = [
                      {
                        key      = "app"
                        operator = "In"
                        values = ["istiod"]
                      }
                    ]
                  }
                  topologyKey = "kubernetes.io/hostname"
                }
              }
            ]
          }
        }
      }
      pilot = {
        autoscaleEnabled = false
        replicaCount     = 1
      }
    })
  ]

  depends_on = [helm_release.istio_base]
}

resource "helm_release" "gateway" {
  name = "gateway"

  repository       = "https://istio-release.storage.googleapis.com/charts"
  chart            = "gateway"
  namespace        = "istio-ingress"
  create_namespace = true
  version          = local.gateway_istio_version
  values = [
    yamlencode({
      service = {
        annotations = {
          "networking.gke.io/load-balancer-type" = "Internal"
        }
        ports = [
          {
            name       = "status-port"
            port       = 15021
            protocol   = "TCP"
            targetPort = 15021
          },
          {
            name       = "http"
            port       = 80
            protocol   = "TCP"
            targetPort = 80
          },
          {
            name       = "https"
            port       = 443
            protocol   = "TCP"
            targetPort = 443
          },
          {
            name       = "secret-manager"
            port       = 3005
            protocol   = "TCP"
            targetPort = 3005
          }
        ]
      }
      autoscaling = {
        enabled = false
      }
      replicaCount = 1
      nodeSelector = {
        "cloud.google.com/gke-nodepool" = "main-pool"
      }
      affinity = {
        nodeAffinity = {
          requiredDuringSchedulingIgnoredDuringExecution = {
            nodeSelectorTerms = [
              {
                matchExpressions = [
                  {
                    key      = "cloud.google.com/gke-nodepool"
                    operator = "In"
                    values = ["main-pool"]
                  }
                ]
              }
            ]
          }
        }
        podAntiAffinity = {
          preferredDuringSchedulingIgnoredDuringExecution = [
            {
              weight = 100
              podAffinityTerm = {
                labelSelector = {
                  matchExpressions = [
                    {
                      key      = "app"
                      operator = "In"
                      values = ["istio-gateway"]
                    }
                  ]
                }
                topologyKey = "kubernetes.io/hostname"
              }
            }
          ]
        }
      }
    })
  ]

  depends_on = [
    helm_release.istio_base,
    helm_release.istiod
  ]
}
