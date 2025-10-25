resource "null_resource" "abs" {
  triggers = {
    always_run = timestamp()
  }

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    working_dir = "${path.root}/ansible"
    command     = <<-EOT
      ansible-playbook \
        generate-abs-conf.yaml \
        --extra-vars='{
          "storage_provider": "${var.storage_provider}",
          "device_type": "${var.device_type}",
          "bucket_name": "${local.bucket_name}",
          "random_path": "${local.random_string}",
          "minio_dns_name": "${local.minio_dns_name}",
          "azure_storage_account": "${data.google_secret_manager_secret_version_access.azure_storage_account.secret_data}",
          "asdb_dns_name": "${local.asdb_dns_name}",
          "gateway_dns_name": "${local.gateway_dns_name}",
          "module_name": "${local.module_name}",
          "service_name": "${local.service_name}",
          "terraform_workspace": "${terraform.workspace}"
        }'
    EOT
  }
}
