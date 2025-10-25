resource "google_compute_instance" "asdb_node" {
  count        = var.nodes.size
  name         = "${terraform.workspace}-${local.module_name}-node-${count.index}"
  machine_type = var.nodes.machine_type
  zone         = data.google_client_config.current.zone

  labels = {
    workspace = terraform.workspace
    service   = local.module_name
  }

  tags = ["http-server", "https-server", local.module_name, "allow-asdb"]

  boot_disk {
    initialize_params {
      image = data.google_compute_image.ubuntu.self_link
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
    subnetwork_project = data.google_compute_subnetwork.asdb.project
    subnetwork         = data.google_compute_subnetwork.asdb.self_link
    stack_type         = "IPV4_ONLY"
    # access_config {}
  }


  service_account {
    email = google_service_account.asdb.email
    scopes = ["cloud-platform"]
  }

  allow_stopping_for_update = true

  desired_status = "RUNNING"
}
