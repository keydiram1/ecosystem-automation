locals {
  module_name = "worker"

  devices = var.worker.devices < 2 && var.worker.machine_type == "n2-standard-16" ? 2 : var.worker.devices

  ansible_vars = jsonencode({
    project_id                = data.google_client_config.current.project
    region                    = data.google_client_config.current.region
    zone                      = data.google_client_config.current.zone
    workspace                 = terraform.workspace
    devices                   = var.worker.devices
    clone_backup_cli_repo   = var.worker.clone_backup_cli_repo
    clone_backup_service_repo = var.worker.clone_backup_service_repo
    clone_backup_library_repo = var.worker.clone_backup_library_repo
    ops_agent                 = var.worker.ops_agent
  })
}
