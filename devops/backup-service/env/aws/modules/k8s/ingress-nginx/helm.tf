resource "helm_release" "ingress_nginx" {
  chart            = "ingress-nginx"
  name             = "ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  version          = var.ingress_nginx.chart_version
  values           = [file(var.ingress_nginx.values)]
  namespace        = var.ingress_nginx.namespace
  create_namespace = true
}