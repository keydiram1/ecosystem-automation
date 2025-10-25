variable "prefix" {
  type = string
}

variable "eks" {
  type = object({
    name           = string
    version        = string
    endpoint       = string
    ca_certificate = string
    openid         = object({
      arn = string
      url = string
    })
  })
}
