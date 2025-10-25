variable "ingress_nginx" {
  type = object({
    chart_version = string
    namespace     = string
    values        = string
  })
}