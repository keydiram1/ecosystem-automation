resource "kubernetes_namespace_v1" "namespace" {
  metadata {
    name = var.namespace
  }
}

resource "kubernetes_manifest" "gateway" {

  manifest = {
    "apiVersion" = "networking.istio.io/v1beta1"
    "kind"       = "Gateway"
    "metadata" = {
      "name"      = "api"
      "namespace" = kubernetes_namespace_v1.namespace.metadata.0.name
    }

    "spec" = {
      "selector" = {
        "istio" = "${terraform.workspace}-${local.module_name}"
      }
      "servers" = [
        {
          "hosts" = [
            data.kubernetes_service_v1.istio_gateway.status.0.load_balancer.0.ingress.0.ip,
          ]
          "port" = {
            "name"     = "http"
            "number"   = 80
            "protocol" = "HTTP"
          }
        },
        {
          "hosts" = [
            data.kubernetes_service_v1.istio_gateway.status.0.load_balancer.0.ingress.0.ip,
          ]
          "port" = {
            "name"     = "secret-manager"
            "number"   = 3005
            "protocol" = "TCP"
          }
        },
      ]
    }
  }
}
