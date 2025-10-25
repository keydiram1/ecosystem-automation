resource "google_iap_client" "client" {
  display_name = "ecosystem-automation"
  brand        = "projects/653670761165/brands/653670761165"
}

resource "google_iap_web_region_backend_service_iam_binding" "binding" {
  project                    = google_compute_region_backend_service.external_backend.project
  region                     = google_compute_region_backend_service.external_backend.region
  web_region_backend_service = google_compute_region_backend_service.external_backend.name
  role                       = "roles/iap.httpsResourceAccessor"
  members                    = local.members
}
