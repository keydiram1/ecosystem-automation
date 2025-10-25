resource "google_service_account" "asa" {
  account_id = "${terraform.workspace}-${local.module_name}-sa"
}

resource "google_project_iam_member" "secret_accessor" {
  project = data.google_client_config.current.project
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.asa.email}"
}

resource "google_project_iam_member" "workload_identity_user" {
  project = data.google_client_config.current.project
  role    = "roles/iam.workloadIdentityUser"
  member  = "serviceAccount:${data.google_client_config.current.project}.svc.id.goog[${data.kubernetes_namespace_v1.namespace.metadata.0.name}/${local.service_name}]"
}
