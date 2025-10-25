data "aws_region" "current" {}

data "kubernetes_namespace_v1" "namespace" {
  metadata {
    name = var.namespace
  }
}

data "kubernetes_service_v1" "istio_gateway" {
  metadata {
    name      = "gateway"
    namespace = "istio-ingress"
  }
}

data "aws_secretsmanager_secret" "regcred" {
  name = "adr/jfrog/dev/creds"
}

data "aws_secretsmanager_secret_version" "regcred" {
  secret_id = data.aws_secretsmanager_secret.regcred.id
}

data "aws_iam_policy_document" "backup_service_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    condition {
      test     = "StringEquals"
      variable = "${replace(var.openid.url, "https://", "")}:sub"
      values   = ["system:serviceaccount:${data.kubernetes_namespace_v1.namespace.metadata.0.name}:backup-service"]
    }

    principals {
      identifiers = [var.openid.arn]
      type        = "Federated"
    }
  }
}

data "aws_iam_policy_document" "backup_service_bucket_access_point_policy" {
  count = var.s3.access_point == true ? 1 : 0

  statement {
    sid     = "AllActions"
    effect  = "Allow"
    actions = [
      "s3:*"
    ]
    resources = ["${data.aws_s3_bucket.bucket.0.arn}", "${data.aws_s3_bucket.bucket.0.arn}/*"]
  }
  statement {
    sid     = "ListBuckets"
    effect  = "Allow"
    actions = [
      "s3:ListAllMyBuckets",
      "s3:HeadBucket"
    ]
    resources = ["*"]
  }
  statement {
    sid     = "AccessPoint"
    effect  = "Allow"
    actions = [
      "s3:*AccessPoint*"
    ]
    resources = [
      var.s3.name
    ]
  }
}

data aws_iam_policy_document "backup_service_policy" {
  count = var.s3.access_point == true ? 0 : 1

  statement {
    sid     = "AllActions"
    effect  = "Allow"
    actions = [
      "s3:*"
    ]
    resources = [data.aws_s3_bucket.bucket.0.arn, "${data.aws_s3_bucket.bucket.0.arn}/*"]
  }
  statement {
    sid     = "ListBuckets"
    effect  = "Allow"
    actions = [
      "s3:ListAllMyBuckets",
      "s3:HeadBucket"
    ]
    resources = ["*"]
  }
}

data "aws_s3_bucket" "bucket" {
  count  = var.s3.access_point == true ? 0 : 1
  bucket = var.s3.name
}

data "aws_secretsmanager_secret_version" "tls" {
  secret_id = data.aws_secretsmanager_secret.tls.id
}

data "aws_secretsmanager_secret" "tls" {
  name = "testenv"
}