resource "google_dns_record_set" "asdb" {
  name         = "asd.${terraform.workspace}.${data.google_dns_managed_zone.dns_zone.dns_name}"
  type         = "A"
  ttl          = 300
  managed_zone = data.google_dns_managed_zone.dns_zone.name
  rrdatas      = [local.ip_address]
}
