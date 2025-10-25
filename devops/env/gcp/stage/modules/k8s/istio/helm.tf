resource "helm_release" "istio_base" {
  name = "${terraform.workspace}-istio-base-release"

  repository       = "https://istio-release.storage.googleapis.com/charts"
  chart            = "base"
  namespace        = "istio-system"
  create_namespace = true
  version          = var.istio.chart_version

  set {
    name  = "defaultRevision"
    value = "default"
  }
}

resource "helm_release" "istiod" {
  name = "${terraform.workspace}-istiod-release"

  repository       = "https://istio-release.storage.googleapis.com/charts"
  chart            = "istiod"
  namespace        = "istio-system"
  create_namespace = true
  version          = var.istio.chart_version

  #  set {
  #    name  = "meshConfig.ingressService"
  #    value = "istio-gateway"
  #  }
  #
  #  set {
  #    name  = "meshConfig.ingressSelector"
  #    value = "gateway"
  #  }

  depends_on = [helm_release.istio_base]
}

resource "helm_release" "gateway" {
  name = "${terraform.workspace}-gateway"

  repository       = "https://istio-release.storage.googleapis.com/charts"
  chart            = "gateway"
  namespace        = "istio-ingress"
  create_namespace = true
  version          = var.istio.chart_version
  values = [
    yamlencode(
      {
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
      }
    )
  ]

  depends_on = [
    helm_release.istio_base,
    helm_release.istiod
  ]
}
