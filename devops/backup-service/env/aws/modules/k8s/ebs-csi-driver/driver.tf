resource "aws_eks_addon" "ebs_csi_driver" {
  cluster_name             = var.eks.name
  addon_name               = local.ebs_csi_driver_name
  addon_version            = data.aws_eks_addon_version.latest.version
  service_account_role_arn = aws_iam_role.role.arn

  tags = {
    Name = "${var.prefix}-ebs-csi-driver"
  }
}
