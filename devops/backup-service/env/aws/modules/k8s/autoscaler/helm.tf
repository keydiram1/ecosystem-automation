resource "helm_release" "autoscaler" {
  name       = local.autoscaler_name
  namespace  = "kube-system"
  chart      = "cluster-autoscaler"
  repository = "https://kubernetes.github.io/autoscaler"
  version    = var.autoscaler.chart_version

  set {
    name  = "autoDiscovery.clusterName"
    value = var.eks.name
  }
  set {
    name  = "awsRegion"
    value = data.aws_region.current.name
  }
  set {
    name  = "rbac.serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = aws_iam_role.role.arn
  }
  set {
    name  = "cloudProvider"
    value = "aws"
  }
  set {
    name  = "image.tag"
    value = var.autoscaler.version
  }
  set {
    name  = "rbac.serviceAccount.name"
    value = local.autoscaler_name
  }
}
