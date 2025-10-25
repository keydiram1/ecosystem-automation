variable "prefix" {
  type = string
}

variable "eks_name" {
  type = string
}

variable "eks_version" {
  type = string
}

variable "eks_private_subnets" {
  type = list(string)
}

variable "eks_public_subnets" {
  type = list(string)
}

variable "node_iam_policies" {
  type    = list(string)
  default = [
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
    "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
  ]
}

variable "node_groups" {
  type = list(object({
    node_group_name = string
    ami_type        = string
    capacity_type   = string
    instance_types  = list(string)
    scaling_config  = object({
      desired_size = number
      max_size     = number
      min_size     = number
    })
    labels = map(any)
  }))
}

variable "enable_irsa" {
  type    = bool
  default = true
}
