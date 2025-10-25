resource "google_compute_instance" "worker" {
  name         = "${terraform.workspace}-${local.module_name}-node"
  machine_type = var.worker.machine_type
  zone         = data.google_client_config.current.zone

  metadata = {
    ssh-keys = "ubuntu:${data.google_secret_manager_secret_version_access.worker_ssh_key_pair_pub.secret_data}"
  }

  labels = {
    workspace = terraform.workspace
    service   = local.module_name
  }

  tags = ["http-server", "https-server", "allow-worker", "allow-xdr-traffic"]

  boot_disk {
    initialize_params {
      image = data.google_compute_image.worker_image.self_link
      size  = 20
      labels = {
        workspace = terraform.workspace
        service   = local.module_name
      }
    }
    auto_delete = true
    device_name = "${terraform.workspace}-${local.module_name}-boot-disk"
    mode        = "READ_WRITE"
  }

  dynamic "scratch_disk" {
    for_each = toset(range(0, local.devices))
    content {
      interface = "NVME"
    }
  }


  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
    stack_type         = "IPV4_ONLY"
  }

  service_account {
    email = google_service_account.worker.email
    scopes = ["cloud-platform"]
  }

  allow_stopping_for_update = true

  desired_status = "RUNNING"

  lifecycle {
    ignore_changes = [attached_disk]
  }
}
