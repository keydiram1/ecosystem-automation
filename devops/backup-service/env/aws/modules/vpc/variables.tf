variable "prefix" {
  type = string
}

variable "vpc_cidr_block" {
  type    = string
  default = "10.0.0.0/16"
}

variable "enable_dns_support" {
  type    = bool
  default = true
}

variable "enable_dns_hostnames" {
  type    = bool
  default = true
}

variable "eks_public_subnets" {
  description = "CIDR ranges for public subnets."
  type        = list(string)
}

variable "eks_private_subnets" {
  description = "CIDR ranges for private subnets."
  type        = list(string)
}

variable "eks_private_subnet_tags" {
  description = "Private subnet tags."
  type        = map(any)
}

variable "eks_public_subnet_tags" {
  description = "Private subnet tags."
  type        = map(any)
}

variable "aerospike_cluster_public_subnets" {
  description = "CIDR ranges for public subnets."
  type        = list(string)
}
