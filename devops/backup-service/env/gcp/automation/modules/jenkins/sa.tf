resource "google_service_account" "this" {
  account_id   = "${var.prefix}-jenkins"
  display_name = "Custom SA for VM Instance"
}

resource "google_project_iam_member" "secret_manager_role" {
  project = data.google_client_config.current.project
  role    = "roles/secretmanager.admin"
  member  = "serviceAccount:${google_service_account.this.email}"
}

resource "google_project_iam_member" "sa_compute_admin" {
  project = data.google_client_config.current.project
  role    = "roles/compute.admin"
  member = "serviceAccount:${google_service_account.this.email}"
}

resource "google_storage_bucket_iam_member" "sa_bucket_access" {
  bucket = data.google_storage_bucket.workspcace_vars_bucket.name
  role   = "roles/storage.objectAdmin" # Allows read/write access to objects in the bucket
  member = "serviceAccount:${google_service_account.this.email}"
}


resource "google_project_iam_member" "sa_key_admin" {
  project = data.google_client_config.current.project
  role    = "roles/iam.serviceAccountKeyAdmin"
  member =  "serviceAccount:${google_service_account.this.email}"
}

resource "google_project_iam_member" "sa_user" {
  project = data.google_client_config.current.project
  role    = "roles/iam.serviceAccountUser"
  member = "serviceAccount:${google_service_account.this.email}"
}

resource "google_project_iam_member" "sa_iam_maneger" {
  project = data.google_client_config.current.project
  role    = "roles/iam.securityAdmin"
  member =  "serviceAccount:${google_service_account.this.email}"

}

resource "google_secret_manager_secret_iam_member" "docker_username_iam" {
  secret_id = data.google_secret_manager_secret.docker_username.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.this.email}"
}

resource "google_secret_manager_secret_iam_member" "docker_password_iam" {
  secret_id = data.google_secret_manager_secret.docker_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.this.email}"
}

resource "google_secret_manager_secret_iam_member" "github_token_iam" {
  secret_id = data.google_secret_manager_secret.github_token.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.this.email}"
}

resource "google_project_iam_member" "bucket_admin" {
  role    = "roles/storage.admin"
  member  = "serviceAccount:${data.google_service_account.abs.email}"
  project = data.google_client_config.current.project
}

resource "google_storage_bucket_iam_member" "bucket_object_user_binding" {
  bucket = "abs-testing-bucket"
  role   = "roles/storage.admin"
  member = "serviceAccount:${data.google_service_account.abs.email}"
}

resource "google_project_iam_member" "sa_gke_admin" {
  project = data.google_client_config.current.project
  role    = "roles/container.admin"
  member  = "serviceAccount:${google_service_account.this.email}"
}

resource "google_project_iam_member" "sa_iap_tunnel_access" {
  project = data.google_client_config.current.project
  role    = "roles/iap.tunnelResourceAccessor"
  member  = "serviceAccount:${google_service_account.this.email}"
}
