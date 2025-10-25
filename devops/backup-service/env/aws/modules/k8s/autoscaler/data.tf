data "aws_region" "current" {}

data "aws_iam_policy_document" "policy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    condition {
      test     = "StringEquals"
      variable = "${replace(var.eks.openid.url, "https://", "")}:sub"
      values   = ["system:serviceaccount:kube-system:${local.autoscaler_name}"]
    }

    principals {
      identifiers = [var.eks.openid.arn]
      type        = "Federated"
    }
  }
}
