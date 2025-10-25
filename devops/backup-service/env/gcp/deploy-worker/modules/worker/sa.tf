resource "google_service_account" "worker" {
  account_id = "${terraform.workspace}-${local.module_name}"
}

resource "google_project_iam_member" "worker_compute_admin" {
  project = data.google_client_config.current.project
  role    = "roles/compute.admin"
  member  = "serviceAccount:${google_service_account.worker.email}"
}

resource "google_project_iam_member" "worker_storage_admin" {
  project = data.google_client_config.current.project
  role    = "roles/storage.admin"
  member  = "serviceAccount:${google_service_account.worker.email}"
}
