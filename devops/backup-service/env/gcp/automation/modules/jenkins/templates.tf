resource "google_compute_instance_template" "jenkins_server" {
  name = local.jenkins_server
  machine_type = var.machine_type
  tags = ["http-server", "https-server", "allow-ssh-and-rp", "load-balanced-backend"]

  labels = {
    name = local.jenkins_server
  }

  metadata = {
    google-logging-enabled = "false"
  }

  disk {
    disk_type    = "pd-standard"
    mode         = "READ_WRITE"
    auto_delete  = true
    disk_size_gb = 50
    source_image = data.google_compute_image.jenkins_server_image.self_link
    boot         = true
  }

  disk {
    source      = data.google_compute_disk.persistent_disk.name
    device_name = "data-disk-0"
    mode        = "READ_WRITE"
    boot        = false
    auto_delete = false
  }
  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    stack_type         = "IPV4_ONLY"
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
  }

  service_account {
    email = google_service_account.this.email
    scopes = ["cloud-platform"]
  }
  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_instance_template" "jenkins_artifact_builder" {
  name = local.jenkins_artifact_builder
  machine_type = "n2-standard-8"
  tags         = ["allow-all", "http-server", "https-server"]

  labels = {
    name = local.jenkins_artifact_builder
  }

  metadata = {
    google-logging-enabled = "false"
  }

  metadata_startup_script = file("${path.module}/scripts/update-snyk.sh")

  disk {
    disk_type    = "pd-standard"
    mode         = "READ_WRITE"
    auto_delete  = true
    disk_size_gb = 20
    source_image = data.google_compute_image.jenkins_artifact_builder.self_link
    boot         = true
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    stack_type         = "IPV4_ONLY"
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
  }

  service_account {
    email = "davidev@ecosystem-connectors-data.iam.gserviceaccount.com"
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_instance_template" "jenkins_gcp_env_test_provisioner" {
  name = local.jenkins_gcp_env_test_provisioner
  machine_type = "e2-custom-4-8192"
  tags         = ["allow-all", "http-server", "https-server"]

  labels = {
    name = local.jenkins_gcp_env_test_provisioner
  }

  metadata = {
    google-logging-enabled = "false"
  }

  disk {
    disk_type    = "pd-standard"
    mode         = "READ_WRITE"
    auto_delete  = true
    disk_size_gb = 20
    source_image = data.google_compute_image.jenkins_gcp_env_test_provisioner_image.self_link
    boot         = true
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    stack_type         = "IPV4_ONLY"
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
  }

  service_account {
    email = "davidev@ecosystem-connectors-data.iam.gserviceaccount.com"
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_instance_template" "jenkins_gcp_test_worker_cli" {
  name = "${local.jenkins_gcp_test_worker}-cli"
  machine_type = "e2-standard-8"
  tags         = ["allow-all", "http-server", "https-server"]

  labels = {
    name = "${local.jenkins_gcp_test_worker}-cli"
  }

  metadata = {
    google-logging-enabled = "false"
  }

  disk {
    disk_type    = "pd-standard"
    mode         = "READ_WRITE"
    auto_delete  = true
    disk_size_gb = 20
    source_image = data.google_compute_image.jenkins_gcp_test_worker_image.self_link
    boot         = true
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    stack_type         = "IPV4_ONLY"
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
  }

  service_account {
    email = google_service_account.this.email
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_instance_template" "jenkins_gcp_test_worker_svc" {
  name = "${local.jenkins_gcp_test_worker}-svc"
  machine_type = "e2-standard-4"
  tags         = ["allow-all", "http-server", "https-server"]

  labels = {
    name = "${local.jenkins_gcp_test_worker}-svc"
  }

  metadata = {
    google-logging-enabled = "false"
  }

  disk {
    disk_type    = "pd-standard"
    mode         = "READ_WRITE"
    auto_delete  = true
    disk_size_gb = 20
    source_image = data.google_compute_image.jenkins_gcp_test_worker_image.self_link
    boot         = true
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    stack_type         = "IPV4_ONLY"
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
  }

  service_account {
    email = google_service_account.this.email
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}


resource "google_compute_instance_template" "jenkins_local_test_worker" {
  name = local.jenkins_local_test_worker
  machine_type = "e2-standard-8"
  tags         = ["allow-all", "http-server", "https-server"]

  labels = {
    name = local.jenkins_local_test_worker
  }

  metadata = {
    google-logging-enabled = "false"
  }

  disk {
    disk_type    = "pd-standard"
    mode         = "READ_WRITE"
    auto_delete  = true
    disk_size_gb = 50
    source_image = data.google_compute_image.jenkins_local_test_worker_image.self_link
    boot         = true
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    stack_type         = "IPV4_ONLY"
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
  }

  service_account {
    email = google_service_account.this.email
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_instance_template" "jenkins_multi_node_local_tests_worker_svc" {
  name = "${local.jenkins_muli_node_local_test_worker}-svc"
  machine_type = "n2d-standard-16"
  tags         = ["allow-all", "http-server", "https-server"]

  labels = {
    name = "${local.jenkins_muli_node_local_test_worker}-svc"
  }

  metadata = {
    google-logging-enabled = "false"
  }

  disk {
    disk_type    = "pd-standard"
    mode         = "READ_WRITE"
    auto_delete  = true
    disk_size_gb = 20
    source_image = data.google_compute_image.jenkins_multi_node_local_test_worker_image.self_link
    boot         = true
  }

  disk {
    disk_type   = "local-ssd"
    disk_size_gb = 375
    type        = "SCRATCH"
    interface = "NVME"
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    stack_type         = "IPV4_ONLY"
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
  }

  service_account {
    email = google_service_account.this.email
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "google_compute_instance_template" "jenkins_multi_node_local_tests_worker_cli" {
  name = "${local.jenkins_muli_node_local_test_worker}-cli"
  machine_type = "n2d-standard-8"
  tags         = ["allow-all", "http-server", "https-server"]

  labels = {
    name = "${local.jenkins_muli_node_local_test_worker}-cli"
  }

  metadata = {
    google-logging-enabled = "false"
  }

  disk {
    disk_type    = "pd-standard"
    mode         = "READ_WRITE"
    auto_delete  = true
    disk_size_gb = 20
    source_image = data.google_compute_image.jenkins_multi_node_local_test_worker_image.self_link
    boot         = true
  }

  disk {
    disk_type   = "local-ssd"
    disk_size_gb = 375
    type        = "SCRATCH"
    interface = "NVME"
  }

  network_interface {
    network            = data.google_compute_network.vpc.self_link
    subnetwork_project = data.google_compute_subnetwork.jenkins.project
    stack_type         = "IPV4_ONLY"
    subnetwork         = data.google_compute_subnetwork.jenkins.self_link
  }

  service_account {
    email = google_service_account.this.email
    scopes = ["cloud-platform"]
  }

  lifecycle {
    create_before_destroy = true
  }
}
