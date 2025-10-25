resource "time_sleep" "wait_40_seconds" {
  create_duration = "40s"

  depends_on = [
    google_compute_instance.asdb_node,
    google_dns_record_set.asdb,
    google_compute_firewall.asdb_internal,
    google_project_iam_member.asdb,
    google_service_account.asdb
  ]
}

resource "null_resource" "asdb" {
  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    working_dir = "${path.root}/ansible"
    command     = <<-EOT

    PROJECT="${data.google_client_config.current.project}" \
    WORKSPACE="${terraform.workspace}" \
    MODULE_NAME="${local.module_name}" \
    envsubst < "./inventory/gcp_compute-tmpl.yaml" > "./inventory/gcp_compute.yaml"

    ansible-playbook \
    install-asdb.yaml \
    --extra-vars "project_id=${data.google_client_config.current.project} \
    region=${data.google_client_config.current.region} \
    zone=${data.google_client_config.current.zone} \
    workspace=${terraform.workspace} \
    asdb_version=${var.nodes.version} \
    distro=${replace(var.nodes.distro, " ", "")} \
    arch=${local.arch[var.nodes.arch]}" \
    --inventory "./inventory/gcp_compute.yaml"
EOT
  }
  depends_on = [time_sleep.wait_40_seconds]
}
