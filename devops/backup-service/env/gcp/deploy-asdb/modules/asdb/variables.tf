variable "prefix" {
  type = string
}

variable "asdb" {
  type = object({
    namespaces           = number
    device_type          = string
    security_type        = string
    section_selection    = list(bool)
    secret_agent         = bool
    devices              = number
    strong_consistency   = bool
    roster               = bool
    ops_agent            = bool
    device_shadow        = bool
    load_balancer        = bool
    encryption_at_rest   = bool
    single_query_threads = number
  })
}

variable "nodes" {
  type = object({
    machine_type = string
    version      = string
    size         = number
    distro       = string
    arch         = string
    multi_zone   = bool
  })
}

