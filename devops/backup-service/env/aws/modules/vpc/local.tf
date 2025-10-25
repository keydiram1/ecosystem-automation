locals {
  dns_name = "${replace(var.prefix, "-", ".")}.internal"
}
