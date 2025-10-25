variable "password_path" {
  type = string
}

variable "namespace" {
  type = string
}

variable "image_tag" {
  type = string
}

variable "prefix" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "openid" {
  type = object({
    arn = string
    url = string
  })
}

variable "s3" {
  type = object({
    name         = string
    access_point = bool
  })
}
