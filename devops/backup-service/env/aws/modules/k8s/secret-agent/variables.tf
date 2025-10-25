variable "prefix" {
  type = string
}

variable "namespace" {
  type = string
}

variable "image_tag" {
  type = string
}

variable "openid" {
  type = object({
    arn = string
    url = string
  })
}