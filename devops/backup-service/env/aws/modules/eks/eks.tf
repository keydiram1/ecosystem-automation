resource "aws_iam_role" "eks" {
  name               = var.eks_name
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Sid       = "EksRole"
        Principal = {
          Service = "eks.amazonaws.com"
        }
      },
    ]
  })

  tags = {
    Name = var.eks_name
  }
}

resource "aws_iam_role_policy_attachment" "eks" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.eks.name
}

resource "aws_eks_cluster" "this" {
  name     = var.eks_name
  version  = var.eks_version
  role_arn = aws_iam_role.eks.arn
  vpc_config {
    endpoint_private_access = false
    endpoint_public_access  = true
    subnet_ids              = concat(var.eks_public_subnets, var.eks_private_subnets)
  }

  tags = {
    Name = var.eks_name
  }

  depends_on = [aws_iam_role_policy_attachment.eks]
}
