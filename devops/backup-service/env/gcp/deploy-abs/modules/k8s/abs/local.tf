resource "random_string" "random" {
  length  = 16
  lower   = true
  numeric = true
  upper   = false
  special = false
}

locals {
  prefix           = terraform.workspace
  module_name      = "abs"
  bucket_name      = "${local.module_name}-testing-bucket"
  asdb_dns_name    = "asd.${terraform.workspace}.${data.google_dns_managed_zone.dns_zone.dns_name}"
  gateway_dns_name = "gateway.${data.google_dns_managed_zone.dns_zone.dns_name}"
  minio_dns_name   = "http://minio.${data.google_dns_managed_zone.dns_zone.dns_name}:9000"
  random_string    = random_string.random.result

  optionalVolumes = var.storage_provider == "minio" ? (
  {
    name = kubernetes_secret_v1.minio_credentials.0.metadata.0.name
    secret = {
      secretName = kubernetes_secret_v1.minio_credentials.0.metadata.0.name
      optional   = true
    }
  }
  ) : var.storage_provider == "s3" ? (
  {
    name = kubernetes_secret_v1.s3_credentials.0.metadata.0.name
    secret = {
      secretName = kubernetes_secret_v1.s3_credentials.0.metadata.0.name
      optional   = true
    }
  }
  ) : null

  # optionalVolumes = null

  volumes = [
    {
      name = kubernetes_secret_v1.tls_secret.metadata.0.name
      secret = {
        secretName = kubernetes_secret_v1.tls_secret.metadata.0.name
        optional   = true
      }
    }
  ]

  finalVolumes = concat(
    local.volumes,
      local.optionalVolumes != null ? [local.optionalVolumes] : []
  )


  # finalVolumes = local.optionalVolumes != null ? (
  # concat(local.volumes, [local.optionalVolumes])
  # ) : (
  # local.volumes
  # )

  extraSecretVolumeMounts = [
    {
      name      = kubernetes_secret_v1.tls_secret.metadata.0.name
      readOnly  = "true"
      mountPath = "/etc/aerospike-backup-service/ssl/ca.aerospike.com.pem"
      subPath   = "ca.aerospike.com.pem"
    }
  ]

  optionalExtraSecretVolumeMounts = var.storage_provider == "minio" || var.storage_provider == "s3" ? (
  {
    name = var.storage_provider == "minio" ? (
    kubernetes_secret_v1.minio_credentials.0.metadata.0.name
    ) : kubernetes_secret_v1.s3_credentials.0.metadata.0.name
    readOnly  = "true"
    mountPath = "/root/.aws/credentials"
    subPath   = "credentials"
  }
  ) : null

  # optionalExtraSecretVolumeMounts = null

  # finalSecretVolumeMounts = local.optionalExtraSecretVolumeMounts != null ? (
  # concat(local.extraSecretVolumeMounts, [local.optionalExtraSecretVolumeMounts])
  # ) : (
  # local.extraSecretVolumeMounts
  # )

  finalSecretVolumeMounts = concat(
    local.extraSecretVolumeMounts,
      local.optionalExtraSecretVolumeMounts != null ? [local.optionalExtraSecretVolumeMounts] : []
  )

  service_name = "aerospike-backup-service"
  config_path  = "config/aerospike-backup-service.yml"
}
