# resource "google_project_iam_member" "bucket_admin" {
#   count   = var.storage == "gcp" ? 1 : 0
#   role    = "roles/storage.admin"
#   member  = "serviceAccount:${data.google_service_account.abs.email}"
#   project = data.google_client_config.current.project
# }
#
# resource "google_storage_bucket_iam_member" "bucket_object_user_binding" {
#   count  = var.storage == "gcp" ? 1 : 0
#   bucket = local.bucket_name
#   role   = "roles/storage.admin"
#   member = "serviceAccount:${data.google_service_account.abs.email}"
# }

resource "google_project_iam_member" "workload_identity_binding" {
  project = data.google_client_config.current.project
  role    = "roles/iam.workloadIdentityUser"
  member  = "serviceAccount:${data.google_client_config.current.project}.svc.id.goog[${data.kubernetes_namespace_v1.namespace.metadata.0.name}/${kubernetes_service_account_v1.abs_sa.metadata.0.name}]"
}
