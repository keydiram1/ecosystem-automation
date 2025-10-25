variable "prefix" {
  type = string
}

variable "autoscaler" {
  type = object({
    chart_version = string
    version       = string
  })
}

variable "eks" {
  type = object({
    name           = string
    endpoint       = string
    ca_certificate = string
    openid         = object({
      arn = string
      url = string
    })
  })
}
