resource "google_compute_forwarding_rule" "forwarding_rule" {
  name                  = "ecoeng-forwording-rule"
  region                = data.google_client_config.current.region
  ip_protocol           = "TCP"
  ip_address            = data.google_compute_address.ip.address
  load_balancing_scheme = "EXTERNAL_MANAGED"
  port_range            = "443"
  target                = google_compute_region_target_https_proxy.proxy.id
  network               = data.google_compute_network.vpc.id
  network_tier          = "STANDARD"
}

resource "google_compute_region_target_https_proxy" "proxy" {
  provider = google-beta
  name     = "ecoeng-http-proxy"
  url_map  = google_compute_region_url_map.url_map.id
  ssl_certificates = [google_compute_region_ssl_certificate.certificate.id]
}

resource "google_compute_region_ssl_certificate" "certificate" {
  provider    = google-beta
  name_prefix = "ecoeng-certificate-"

  region      = data.google_client_config.current.region
  private_key = data.google_secret_manager_secret_version_access.privkey_pem.secret_data
  certificate = data.google_secret_manager_secret_version_access.fullchain_pem.secret_data

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_region_url_map" "url_map" {
  provider        = google-beta
  name            = "${var.prefix}-url-map"
  region          = data.google_client_config.current.region
  default_service = google_compute_region_backend_service.external_backend.id

  host_rule {
    hosts = ["ecoeng.dev"]
    path_matcher = "allpaths"
  }

  path_matcher {
    name            = "allpaths"
    default_service = google_compute_region_backend_service.external_backend.id

    path_rule {
      paths = ["/*"]
      service = google_compute_region_backend_service.external_backend.id
    }
  }
}

resource "google_compute_region_backend_service" "external_backend" {
  name                  = "${var.prefix}-external-backend"
  region                = data.google_client_config.current.region
  protocol              = "HTTP"
  port_name             = "http"
  load_balancing_scheme = "EXTERNAL_MANAGED"
  timeout_sec           = 10
  health_checks = [google_compute_region_health_check.hc.id]
  backend {
    group           = google_compute_instance_group_manager.vm_group_manager.instance_group
    balancing_mode  = "UTILIZATION"
    capacity_scaler = 1.0
  }
  iap {
    enabled              = true
    oauth2_client_id     = google_iap_client.client.client_id
    oauth2_client_secret = google_iap_client.client.secret
  }
}

resource "google_compute_region_health_check" "hc" {
  provider = google-beta
  name     = "${var.prefix}-hc"
  region   = data.google_client_config.current.region
  check_interval_sec = 30
  http_health_check {
    port         = 8080
    request_path = "/healthz"
  }
}

resource "google_dns_record_set" "a" {
  name         = "automation.${data.google_dns_managed_zone.dns_public_zone.dns_name}"
  managed_zone = data.google_dns_managed_zone.dns_public_zone.name
  type         = "A"
  ttl          = 300
  rrdatas = [data.google_compute_address.ip.address]
}

resource "google_dns_record_set" "rp_instance" {
  name         = "rp.${data.google_dns_managed_zone.dns_private_zone.dns_name}"
  managed_zone = data.google_dns_managed_zone.dns_private_zone.name
  type         = "A"
  ttl          = 300
  rrdatas = [data.google_compute_instance.jenkins_backend.network_interface.0.network_ip]
}

resource "google_dns_record_set" "jenkins_instance" {
  name         = "jenkins.${data.google_dns_managed_zone.dns_private_zone.dns_name}"
  managed_zone = data.google_dns_managed_zone.dns_private_zone.name
  type         = "A"
  ttl          = 300
  rrdatas = [data.google_compute_instance.jenkins_backend.network_interface.0.network_ip]
}
