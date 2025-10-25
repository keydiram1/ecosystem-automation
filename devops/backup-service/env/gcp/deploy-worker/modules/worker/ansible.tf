resource "time_sleep" "wait_20_seconds" {
  create_duration = "20s"

  depends_on = [
    google_compute_instance.worker,
    google_dns_record_set.asdb,
    google_compute_firewall.worker,
    google_project_iam_member.worker_compute_admin,
    google_project_iam_member.worker_storage_admin,
    google_service_account.worker
  ]
}

resource "null_resource" "worker" {
  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    working_dir = "${path.root}/ansible"
    command     = <<-EOT
		set -euo pipefail

		export PROJECT="${data.google_client_config.current.project}"
		export WORKSPACE="${terraform.workspace}"
		export MODULE_NAME="${local.module_name}"

		envsubst < "./inventory/gcp_compute-tmpl.yaml" > "./inventory/gcp_compute.yaml"

		ansible-playbook \
		  generate-group-vars.yaml \
		  --extra-vars="workspace=${terraform.workspace} service_name=${local.module_name} local_interpreter=$(which python3)"

        ansible-playbook init-worker.yaml \
          --extra-vars '${local.ansible_vars}' \
          --inventory "./inventory/gcp_compute.yaml"
	EOT
  }

  depends_on = [time_sleep.wait_20_seconds]
}
