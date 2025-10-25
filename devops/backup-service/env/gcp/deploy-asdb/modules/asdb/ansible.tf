resource "time_sleep" "wait_40_seconds" {
  create_duration = "40s"

  depends_on = [
    google_compute_instance.asdb_node,
    google_compute_firewall.asdb_internal,
    google_project_iam_member.asdb,
    google_service_account.asdb,
    google_dns_record_set.asdb
  ]
}

resource "null_resource" "asdb" {
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

        ANSIBLE_FORKS="${var.nodes.size}" ansible-playbook install-asdb.yaml \
          --extra-vars '${local.ansible_vars}' \
          --inventory "./inventory/gcp_compute.yaml"
	EOT
  }

  depends_on = [time_sleep.wait_40_seconds]
}
