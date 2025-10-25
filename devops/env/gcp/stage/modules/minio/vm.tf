resource "google_compute_instance" "minio" {
  name         = local.module_name
  machine_type = var.machine_type
  zone         = data.google_client_config.current.zone

  lifecycle {
    ignore_changes = [boot_disk]
  }

  labels = {
    workspace = terraform.workspace
    service   = local.module_name
  }

  tags = ["http-server", "https-server", local.module_name]

  shielded_instance_config {
    enable_secure_boot          = false
    enable_vtpm                 = false
    enable_integrity_monitoring = false
  }

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2404-lts-amd64"
      size  = 30
      labels = {
        workspace = terraform.workspace
        service   = local.module_name
      }

    }
    auto_delete = true
    device_name = "${terraform.workspace}-${local.module_name}-boot-disk"
    mode        = "READ_WRITE"
  }

  scratch_disk {
    interface = "NVME"
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.minio.project
    subnetwork         = data.google_compute_subnetwork.minio.self_link
    stack_type         = "IPV4_ONLY"
  }

  service_account {
    email = google_service_account.minio.email
    scopes = ["cloud-platform"]
  }

  allow_stopping_for_update = true

  desired_status = "RUNNING"
}
