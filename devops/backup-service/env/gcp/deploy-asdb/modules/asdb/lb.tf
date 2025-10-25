resource "google_compute_region_backend_service" "internal_nlb_backend" {
  count                 = var.asdb.load_balancer ? 1 : 0
  name                  = "${terraform.workspace}-${local.module_name}-nlb-backend-service"
  region                = data.google_client_config.current.region
  protocol              = "TCP"
  load_balancing_scheme = "INTERNAL"
  health_checks         = [google_compute_region_health_check.asdb_tcp_hc[0].self_link]

  session_affinity                = "NONE"
  connection_draining_timeout_sec = 10

  dynamic "backend" {
    for_each = google_compute_instance_group.asdb_nodes
    content {
      group          = backend.value.self_link
      balancing_mode = "CONNECTION"
    }
  }
}

resource "google_compute_forwarding_rule" "internal_nlb" {
  count                 = var.asdb.load_balancer ? 1 : 0
  name                  = "${terraform.workspace}-${local.module_name}-nlb-forwarding-rule"
  region                = data.google_client_config.current.region
  load_balancing_scheme = "INTERNAL"
  backend_service       = google_compute_region_backend_service.internal_nlb_backend[0].self_link
  ip_protocol           = "TCP"
  ports                 = [local.service_port]

  network    = data.google_compute_network.vpc.id
  subnetwork = data.google_compute_subnetwork.asdb.id
}
