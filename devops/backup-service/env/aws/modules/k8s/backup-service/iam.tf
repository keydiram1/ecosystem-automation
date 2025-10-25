resource "aws_iam_role" "backup_service_role" {
  assume_role_policy = data.aws_iam_policy_document.backup_service_assume_role_policy.json
  name               = "${var.prefix}-${local.service_name}-iam-role"

  tags = {
    Name = "${var.prefix}-${local.service_name}-iam-role"
  }
}

resource "aws_iam_policy" "backup_service_policy" {
  name   = "${var.prefix}-${local.service_name}-iam-policy"
  policy = var.s3.access_point == true ? data.aws_iam_policy_document.backup_service_bucket_access_point_policy.0.json : data.aws_iam_policy_document.backup_service_policy.0.json

  tags = {
    Name = "${var.prefix}-${local.service_name}-iam-policy"
  }
}

#resource "aws_iam_role_policy_attachment" "backup_service_attachment" {
#  role       = aws_iam_role.backup_service_role.name
#  policy_arn = aws_iam_policy.backup_service_policy.arn
#}

resource "aws_iam_role_policy_attachment" "backup_service_attachment" {
  role       = aws_iam_role.backup_service_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}