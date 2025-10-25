data "aws_region" "current" {}

data "kubernetes_namespace_v1" "namespace" {
  metadata {
    name = var.namespace
  }
}

data "aws_iam_policy_document" "secret_agent_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    condition {
      test     = "StringEquals"
      variable = "${replace(var.openid.url, "https://", "")}:sub"
      values   = ["system:serviceaccount:${data.kubernetes_namespace_v1.namespace.metadata.0.name}:secret-agent"]
    }

    principals {
      identifiers = [var.openid.arn]
      type        = "Federated"
    }
  }
}

data "kubernetes_service_v1" "istio_gateway" {
  metadata {
    name = "gateway"
    namespace = "istio-ingress"
  }
}

data aws_iam_policy_document "secret_manager_policy" {
  statement {
    sid     = "GetSecrets"
    effect  = "Allow"
    actions = ["secretsmanager:GetSecretValue"]
    resources = [data.aws_secretsmanager_secret.tls.arn]
  }
}

data "aws_secretsmanager_secret_version" "tls" {
  secret_id = data.aws_secretsmanager_secret.tls.id
}

data "aws_secretsmanager_secret" "tls" {
  name = "testenv"
}
