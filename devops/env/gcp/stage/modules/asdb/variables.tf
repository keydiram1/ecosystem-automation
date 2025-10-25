variable "prefix" {
  type = string
}

variable "nodes" {
  type = object({
    machine_type = string
    version      = string
    size         = number
    distro          = string
    arch = string
  })
}
