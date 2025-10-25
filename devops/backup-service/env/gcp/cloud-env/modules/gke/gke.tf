resource "google_container_cluster" "gke" {
  name                     = var.cluster_name
  location                 = data.google_client_config.current.zone
  remove_default_node_pool = true
  initial_node_count       = 1
  deletion_protection      = false
  networking_mode          = "VPC_NATIVE"
  network                  = data.google_compute_network.vpc.self_link
  subnetwork               = data.google_compute_subnetwork.gke.self_link

  release_channel {
    channel = "REGULAR"
  }

  ip_allocation_policy {
    cluster_secondary_range_name  = "pod-ip-range"
    services_secondary_range_name = "service-ip-range"
  }

  network_policy {
    provider = "PROVIDER_UNSPECIFIED"
    enabled  = true
  }

  private_cluster_config {
    enable_private_endpoint = false
    enable_private_nodes = true
    # TODO Replace the cidr block with what defined in the vpc instead of hardcoding it
    master_ipv4_cidr_block  = "172.16.32.0/28"
  }

  workload_identity_config {
    workload_pool = "${data.google_client_config.current.project}.svc.id.goog"
  }
}

# resource "google_container_node_pool" "aerospike_secret_agent_pool" {
#   name = "aerospike-secret-agent-pool"
#   location = data.google_client_config.current.zone
#   cluster = google_container_cluster.gke.name
#   node_count = 1
#
#   management {
#     auto_repair  = true
#     auto_upgrade = true
#   }
#
#   node_config {
#     preemptible  = false
#     machine_type = "e2-small"
#     labels = {
#       role = "aerospike-secret-agent"
#     }
#
#     service_account = google_service_account.gke.email
#     oauth_scopes = [
#       "https://www.googleapis.com/auth/cloud-platform"
#     ]
#     tags = ["http-server", "https-server", local.module_name]
#   }
#
#   network_config {
#     enable_private_nodes = true
#   }
#
# }

resource "google_container_node_pool" "node_pool" {
  for_each = {for idx, pool in var.node_pools : pool.pool_name => pool}
  name     = each.value.pool_name
  location = data.google_client_config.current.zone
  cluster  = google_container_cluster.gke.name

  autoscaling {
    total_min_node_count = each.value.autoscaling.min_size
    total_max_node_count = each.value.autoscaling.max_size
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  node_config {
    preemptible  = false
    machine_type = each.value.machine_type
    labels = {
      role = each.value.role_label
    }

    service_account = google_service_account.gke.email
    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]
    tags = ["http-server", "https-server", local.module_name]
  }

  network_config {
    enable_private_nodes = true
  }
}
