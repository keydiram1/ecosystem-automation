resource "google_compute_instance" "asdb_node" {
  count        = var.nodes.size
  name         = "${terraform.workspace}-${local.module_name}-node-${count.index}"
  machine_type = var.nodes.machine_type

  zone = element(local.rr_zones, count.index % length(local.rr_zones))

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

  dynamic "scratch_disk" {
    for_each = toset(range(0, local.devices))
    content {
      interface = "NVME"
    }
  }

  dynamic "attached_disk" {
    for_each = (var.asdb.device_shadow && !var.nodes.multi_zone) ? [1] : []
    content {
      source = data.google_compute_disk.shadow_disk_0[count.index].self_link
      mode   = "READ_WRITE"
    }
  }

  dynamic "attached_disk" {
    for_each = (var.asdb.device_shadow && !var.nodes.multi_zone) ? [1] : []
    content {
      source = data.google_compute_disk.shadow_disk_1[count.index].self_link
      mode   = "READ_WRITE"
    }
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.asdb.project
    subnetwork         = data.google_compute_subnetwork.asdb.self_link
    stack_type         = "IPV4_ONLY"
  }

  service_account {
    email  = google_service_account.asdb.email
    scopes = ["cloud-platform"]
  }

  allow_stopping_for_update = true
  desired_status            = "RUNNING"

  lifecycle {
    ignore_changes = [attached_disk]
  }
}

resource "google_compute_instance_group" "asdb_nodes" {
  for_each = var.asdb.load_balancer ? toset([for i in google_compute_instance.asdb_node : i.zone]) : []

  name = "${terraform.workspace}-asdb-nodes-${each.key}"
  zone = each.key

  instances = [
    for i in google_compute_instance.asdb_node : i.self_link
    if i.zone == each.key
  ]

  named_port {
    name = "svc"
    port = local.service_port
  }

  named_port {
    name = "heartbeat"
    port = local.heartbeat_port
  }

  named_port {
    name = "fabric"
    port = local.fabric_port
  }
}


resource "google_compute_region_health_check" "asdb_tcp_hc" {
  count  = var.asdb.load_balancer ? 1 : 0
  name   = "${terraform.workspace}-${local.module_name}-tcp-hc"
  region = data.google_compute_subnetwork.asdb.region

  timeout_sec         = 5
  check_interval_sec  = 5
  healthy_threshold   = 2
  unhealthy_threshold = 2

  tcp_health_check {
    port = local.service_port
  }
}
