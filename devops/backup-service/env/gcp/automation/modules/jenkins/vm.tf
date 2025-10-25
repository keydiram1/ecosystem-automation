# resource "google_compute_disk" "pd" {
#   name = "${local.instance_name}-data-disk"
#   project = data.google_client_config.current.project
#   type = "pd-standard"
#   zone = data.google_client_config.current.zone
#   size = 20
# }

resource "google_compute_instance_group_manager" "vm_group_manager" {
  name = "ecoeng-vm-group-manager"
  version {
    instance_template = google_compute_instance_template.jenkins_server.self_link
  }
  base_instance_name = "${var.prefix}-server"
  zone               = data.google_client_config.current.zone
  target_size        = "1"
  wait_for_instances = true
  named_port {
    name = "http"
    port = 8080
  }

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    working_dir = "${path.cwd}/ansible"
    command     = <<-EOT
      ansible-playbook -i "./inventory/gcp_compute.yaml" "startup.yaml"
    EOT
  }
}
