variable "prefix" {
  type = string
}

variable "metric_server" {
  type = object({
    chart_version = string
  })
}
