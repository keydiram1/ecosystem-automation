resource "aws_iam_role" "nodes" {
  name               = "${var.prefix}-eks-nodes"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Sid       = ""
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      },
    ]
  })

  tags = {
    Name = "${var.prefix}-eks-nodes"
  }
}

resource "aws_iam_role_policy_attachment" "nodes" {
  for_each   = toset(var.node_iam_policies)
  policy_arn = each.value
  role       = aws_iam_role.nodes.name
}

resource "aws_eks_node_group" "this" {
  for_each = {for ng in var.node_groups : ng.node_group_name => ng}
  cluster_name    = aws_eks_cluster.this.name
  version         = aws_eks_cluster.this.version
  release_version = nonsensitive(data.aws_ssm_parameter.eks_ami_release_version.value)
  node_group_name = each.value.node_group_name
  node_role_arn   = aws_iam_role.nodes.arn
  subnet_ids = concat(var.eks_private_subnets, var.eks_public_subnets)
  capacity_type  = each.value.capacity_type
  instance_types = each.value.instance_types
  labels = each.value.labels

  scaling_config {
    desired_size = each.value.scaling_config.desired_size
    max_size     = each.value.scaling_config.max_size
    min_size     = each.value.scaling_config.min_size
  }

  update_config {
    max_unavailable = 1
  }

  lifecycle {
    ignore_changes = [scaling_config.0.desired_size]
  }

  depends_on = [aws_iam_role_policy_attachment.nodes]

  tags = {
    Name = each.value.node_group_name
  }
}
