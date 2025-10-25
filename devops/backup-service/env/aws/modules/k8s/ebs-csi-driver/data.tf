data "aws_eks_addon_version" "latest" {
  addon_name         = local.ebs_csi_driver_name
  kubernetes_version = var.eks.version
  most_recent        = true
}

data "aws_iam_policy_document" "policy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    condition {
      test     = "StringEquals"
      variable = "${replace(var.eks.openid.url, "https://", "")}:sub"
      values   = ["system:serviceaccount:kube-system:ebs-csi-controller-sa"]
    }

    principals {
      identifiers = [var.eks.openid.arn]
      type        = "Federated"
    }
  }
}
