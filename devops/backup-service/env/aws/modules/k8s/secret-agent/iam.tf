resource "aws_iam_role" "secret_agent_role" {
  assume_role_policy = data.aws_iam_policy_document.secret_agent_assume_role_policy.json
  name               = "${var.prefix}-${local.service_name}-iam-role"

  tags = {
    Name = "${var.prefix}-${local.service_name}-iam-role"
  }
}

resource "aws_iam_policy" "secret_agent_policy" {
  name   = "${var.prefix}-${local.service_name}-iam-policy"
  policy = data.aws_iam_policy_document.secret_manager_policy.json

  tags = {
    Name = "${var.prefix}-${local.service_name}-iam-policy"
  }
}

resource "aws_iam_role_policy_attachment" "secret-agent_attachment" {
  role       = aws_iam_role.secret_agent_role.name
  policy_arn = aws_iam_policy.secret_agent_policy.arn
}

#resource "aws_iam_role_policy_attachment" "backup_service_attachment" {
#  role       = aws_iam_role.backup_service_role.name
#  policy_arn = "arn:aws:iam::aws:policy/SecretsManagerReadWrite"
#}