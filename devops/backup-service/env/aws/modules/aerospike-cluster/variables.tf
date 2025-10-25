variable "cluster_name" {
  type = string
}

variable "nodes" {
  type = object({
    instance_type = string
    size = number
    version = string
  })
}

variable "subnet_id" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "sg_id" {
  type = string
}

variable "prefix" {
  type = string
}
