resource "google_service_account" "aerospike_secret_agent" {
  account_id = "${local.module_name}"
}

resource "google_project_iam_member" "aerospike_secret_agent_secret_accessor" {
  project = data.google_client_config.current.project
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.aerospike_secret_agent.email}"
}

resource "google_project_iam_member" "aerospike_secret_agent_workload_identity" {
  project = data.google_client_config.current.project
  role    = "roles/iam.workloadIdentityUser"
  member  = "serviceAccount:${data.google_client_config.current.project}.svc.id.goog[${data.kubernetes_namespace_v1.namespace.metadata.0.name}/${local.module_name}]"
}
