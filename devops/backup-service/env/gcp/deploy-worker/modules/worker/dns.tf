resource "google_dns_record_set" "asdb" {
  name         = "worker.${terraform.workspace}.${data.google_dns_managed_zone.dns_zone.dns_name}"
  type         = "A"
  ttl          = 300
  managed_zone = data.google_dns_managed_zone.dns_zone.name
  rrdatas = [google_compute_instance.worker.network_interface.0.network_ip]

}
