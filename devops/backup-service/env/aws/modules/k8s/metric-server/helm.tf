resource "helm_release" "metric_server" {
  chart      = "metrics-server"
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server"
  namespace  = "kube-system"
  version    = var.metric_server.chart_version
}
