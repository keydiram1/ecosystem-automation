resource "time_sleep" "wait_40_seconds" {
  create_duration = "40s"

  depends_on = [
    google_compute_instance.minio,
    google_dns_record_set.minio,
    google_service_account.minio,
    google_project_iam_member.minio
  ]
}

resource "null_resource" "minio" {

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    working_dir = "${path.root}/ansible"
    command     = <<-EOT

    PROJECT="${data.google_client_config.current.project}" \
    WORKSPACE="${terraform.workspace}" \
    MODULE_NAME="${local.module_name}" \
    envsubst < "./inventory/gcp_compute-tmpl.yaml" > "./inventory/gcp_compute.yaml"

    ansible-playbook \
    install.yaml \
    --extra-vars "project_id=${data.google_client_config.current.project} \
    region=${data.google_client_config.current.region} \
    zone=${data.google_client_config.current.zone} \
    workspace=${terraform.workspace}" \
    --inventory "./inventory/gcp_compute.yaml"
EOT
  }
  depends_on = [time_sleep.wait_40_seconds]
}
