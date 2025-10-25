variable "prefix" {
  type = string
}

variable "worker" {
  type = object({
    machine_type              = string
    devices                   = number
    ops_agent                 = bool
    clone_backup_cli_repo   = bool
    clone_backup_service_repo = bool
    clone_backup_library_repo = bool
  })
}
