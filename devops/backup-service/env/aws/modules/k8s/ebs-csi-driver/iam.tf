resource "aws_iam_role" "role" {
  assume_role_policy = data.aws_iam_policy_document.policy.json
  name               = "${var.prefix}-ebs-csi-driver-role"

  tags = {
    Name = "${var.prefix}-ebs-csi-driver-role"
  }
}

resource "aws_iam_role_policy_attachment" "attachment" {
  role       = aws_iam_role.role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
}
