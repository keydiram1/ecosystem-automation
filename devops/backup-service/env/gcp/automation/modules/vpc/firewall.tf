resource "google_compute_firewall" "iap" {
  name      = "${var.prefix}-allow-iap"
  network   = google_compute_network.vpc.self_link
  direction = "INGRESS"
  project   = data.google_client_config.current.project
  source_ranges = ["35.235.240.0/20"]

  allow {
    protocol = "tcp"
    ports = ["22"]
  }
}

resource "google_compute_firewall" "allow_all_ports" {
  # TODO Should be changed
  name    = "${var.prefix}-allow-all-ports"
  network = google_compute_network.vpc.self_link # Replace with your network name if not using the default

  allow {
    protocol = "tcp"
    ports = ["0-65535"]
  }

  allow {
    protocol = "udp"
    ports = ["0-65535"]
  }

  allow {
    protocol = "icmp"
  }

  source_ranges = ["0.0.0.0/0"]  # Allows from any source; change this to restrict access
  target_tags = ["allow-all"]  # Replace with the appropriate tag for your instances, if needed
}

# resource "google_compute_firewall" "internal_ssh" {
#   name    = "${var.prefix}-allow-internal-ssh"
#   network = google_compute_network.vpc.self_link
#   project = data.google_client_config.current.project
#
#   allow {
#     protocol = "tcp"
#     ports = ["22"]
#   }
#
#   allow {
#     protocol = "tcp"
#     ports = ["80", "8080",]
#   }
#
#
#   source_ranges = [local.primary_ip["jenkins"]]
# }

# resource "google_compute_firewall" "allow_http_https" {
#   name    = "allow-http-https"
#   network = google_compute_network.main.self_link
#   project = data.google_client_config.current.project
#
#   allow {
#     protocol = "tcp"
#     ports = ["80", "443", "8080"]
#   }
#   target_tags = ["${var.prefix}-automation"]
#   source_ranges = ["0.0.0.0/0"]
# }

# resource "google_compute_firewall" "fw1" {
#   name    = "website-fw-1"
#   network = google_compute_network.main.id
#   source_ranges = [local.primary_ip["public"], local.primary_ip["private"]]
#   allow {
#     protocol = "tcp"
#   }
#   allow {
#     protocol = "udp"
#   }
#   allow {
#     protocol = "icmp"
#   }
#   direction = "INGRESS"
# }

resource "google_compute_firewall" "fw2" {
  #   depends_on = [google_compute_firewall.fw1]
  name    = "${var.prefix}-website-fw-2"
  network = google_compute_network.vpc.id
  source_ranges = ["130.211.0.0/22", "35.191.0.0/16"]
  allow {
    protocol = "tcp"
    ports = ["80"]
  }
  allow {
    protocol = "tcp"
    ports = ["443"]
  }
  allow {
    protocol = "tcp"
    ports = ["8080"]
  }
  target_tags = ["load-balanced-backend"]
  direction = "INGRESS"
}

resource "google_compute_firewall" "fw3" {
  depends_on = [google_compute_firewall.fw2]
  name    = "${var.prefix}-website-fw-3"
  network = google_compute_network.vpc.id
  source_ranges = [local.primary_ip["proxy"]]
  target_tags = ["load-balanced-backend"]
  allow {
    protocol = "tcp"
    ports = ["80"]
  }
  allow {
    protocol = "tcp"
    ports = ["443"]
  }
  allow {
    protocol = "tcp"
    ports = ["8080"]
  }
  direction = "INGRESS"
}
