resource "google_service_account" "asdb" {
  account_id = "${terraform.workspace}-${local.module_name}-sa"
}

resource "google_project_iam_member" "asdb" {
  project = data.google_client_config.current.project
  role    = "roles/compute.admin"
  member  = "serviceAccount:${google_service_account.asdb.email}"
}
