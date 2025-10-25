output "eks_name" {
  value = aws_eks_cluster.this.name
}

output "eks_version" {
  value = aws_eks_cluster.this.version
}

output "openid_provider_arn" {
  value = aws_iam_openid_connect_provider.this.0.arn
}

output "openid_provider_url" {
  value = aws_iam_openid_connect_provider.this.0.url
}

output "endpoint" {
  value = aws_eks_cluster.this.endpoint
}

output "ca_certificate" {
  value = aws_eks_cluster.this.certificate_authority[0].data
}

output "issuer" {
  value = aws_eks_cluster.this.identity[0].oidc[0].issuer
}
