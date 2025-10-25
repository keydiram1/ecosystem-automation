resource "random_string" "random" {
  length  = 16
  lower   = true
  numeric = true
  upper   = false
  special = false
}

locals {
  prefix      = terraform.workspace
  module_name = "abs"
  bucket_name = "${local.module_name}-testing-bucket"
  dns_name = trimsuffix(data.google_dns_managed_zone.dns_zone.dns_name, ".")

  minio_storage = {
    s3-storage = {
      s3-region            = "eu-central-1"
      s3-profile           = "minio"
      s3-endpoint-override = "http://minio.${local.dns_name}:9000"
      bucket               = local.bucket_name
      path                 = random_string.random.result
    }
  }

  s3_storage = {
    s3-storage = {
      bucket     = local.bucket_name
      path       = random_string.random.result
      s3-region  = "il-central-1"
      # access-key-id     = "secrets:awsAccessKey:aws-access-key-id"
      # secret-access-key = "secrets:awsSecretAccessKey:aws-secret-access-key"
      # secret-agent-name = "absSecretAgent"
      s3-profile = "s3"
    }
  }

  gcs_storage = {
    gcp-storage = {
      bucket-name = local.bucket_name
      path        = random_string.random.result
      # key-json          = "secrets:gcpSaKeyFile:gcp-sa-key-file"
      # secret-agent-name = "absSecretAgent"
    }
  }

  azure_storage = {
    azure-storage = {
      endpoint       = "https://${data.google_secret_manager_secret_version_access.azure_storage_account.secret_data}.blob.core.windows.net"
      container-name = local.bucket_name
      path           = random_string.random.result
      # tenant-id         = "secrets:azureTenantID:azure-tenant-id"
      # client-id         = "secrets:azureClientID:azure-client-id"
      # client-secret     = "secrets:azureClientSecret:azure-client-secret"
      # secret-agent-name = "absSecretAgent"
    }
  }

  storage = var.storage == "gcp" ? (
  local.gcs_storage
  ) : var.storage == "minio" ? (
  local.minio_storage
  ) : var.storage == "s3" ? (
  local.s3_storage
  ) : var.storage == "azure" ? (
  local.azure_storage
  ) : null

  optionalVolumes = var.storage == "minio" ? (
  {
    name = kubernetes_secret_v1.minio_credentials.0.metadata.0.name
    secret = {
      secretName = kubernetes_secret_v1.minio_credentials.0.metadata.0.name
      optional   = true
    }
  }
  ) : var.storage == "s3" ? (
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

  optionalExtraSecretVolumeMounts = var.storage == "minio" || var.storage == "s3" ? (
  {
    name = var.storage == "minio" ? (
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

  config = {
    backupServiceConfig = {
      service = {
          logger = {
            level = "INFO"
          }
      }
      aerospike-clusters = {
        absDefaultCluster = {
          label = "asd.aerospike.com"
          seed-nodes = [
            {
              host-name = "asd.${local.dns_name}"
              port      = 4333
              tls-name  = "asd.aerospike.com"
            }
          ]
          credentials = {
            secret-agent-name = "absSecretAgent"
            # secret-agent  = "absSecretAgent"
            user     = "tester"
            password = "secrets:asdPsw:password-txt"
            # password-path = "/config/password.txt"
          }
          tls = {
            name    = "asd.aerospike.com"
            ca-file = "/etc/aerospike-backup-service/ssl/ca.aerospike.com.pem"
          }
          max-parallel-scans = 5
        }
      }
      secret-agents = {
        absSecretAgent = {
          connection-type = "tcp"
          address         = "gateway.${local.dns_name}"
          port            = 3005
          is-base64       = true
        }
      }

      storage = {
        backup-storage = local.storage
      }

      backup-policies = {
        encryptedCompressedSecretAgentPolicy = {
          parallel = 8
          retention = {
            full        = 1
            incremental = 0
          }
          sealed = true
          encryption = {
            key-secret = "secrets:encKey:encryption-key-pem"
            mode : "AES256"
          }
          compression = {
            level = 20
            mode  = "ZSTD"
          }
        }
        defaultPolicy = {
          parallel = 8
          sealed   = true
          with-cluster-configuration = true
          retention = {
            full        = 10
            incremental = 10
          }
        }
        withBandwidthLimit = {
          parallel  = 8
          bandwidth = 10
          sealed    = true
        }
        xdrBackup = {
          parallel = 8
          retention = {
            full        = 10
            incremental = 10
          }
          xdr = {
            local-host = "${helm_release.backup_service.name}-${local.service_name}-${data.kubernetes_namespace_v1.namespace.metadata.0.name}.svc.cluster.local"
          }
        }
      }

      backup-routines = {
        minio = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns4"]
          backup-policy  = "defaultPolicy"
        }
        edgeCases = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns7"]
          backup-policy  = "defaultPolicy"
        }
        filterBySetAndBin = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns8"]
          set-list = ["backupSet"]
          bin-list = ["backupBin"]
          backup-policy  = "defaultPolicy"
        }
        localStorageIncremental3 = {
          interval-cron      = "@yearly"
          incr-interval-cron = "*/10 * * * * *"
          source-cluster     = "absDefaultCluster"
          storage            = "backup-storage"
          namespaces = ["source-ns10"]
          backup-policy      = "default"
        }
        fullBackup1 = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns9"]
          backup-policy  = "defaultPolicy"
        }
        fullBackup2 = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns11"]
          backup-policy  = "defaultPolicy"
        }
        fullBackup3 = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns12"]
          backup-policy  = "defaultPolicy"
        }
        timestamp = {
          interval-cron      = "@yearly"
          incr-interval-cron = "*/10 * * * * *"
          source-cluster     = "absDefaultCluster"
          storage            = "backup-storage"
          namespaces = ["source-ns14"]
          backup-policy      = "defaultPolicy"
        }
        fullBackupSlow = {
          interval-cron  = "*/20 * * * * *"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns13"]
          backup-policy  = "withBandwidthLimit"
        }
        incrementalBackupCluster = {
          interval-cron      = "@yearly"
          incr-interval-cron = "*/45 * * * * *"
          source-cluster     = "absDefaultCluster"
          storage            = "backup-storage"
          namespaces = []
          backup-policy      = "defaultPolicy"
        }
        incrementalBackupMultipleNSs = {
          interval-cron      = "@yearly"
          incr-interval-cron = "*/30 * * * * *"
          source-cluster     = "absDefaultCluster"
          storage            = "backup-storage"
          namespaces = ["source-ns18", "source-ns19", "source-ns20"]
          backup-policy      = "defaultPolicy"
        }
        fullBackupFullCluster = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = []
          backup-policy  = "defaultPolicy"
        }
        fullBackup3Namespaces = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns15", "source-ns16", "source-ns17"]
          backup-policy  = "defaultPolicy"
        }
        fullBackup4 = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns21"]
          backup-policy  = "defaultPolicy"
        }
        fullBackup5 = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns22"]
          backup-policy  = "defaultPolicy"
        }
        fullBackup6 = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns23"]
          backup-policy  = "defaultPolicy"
        }
        fullBackupEncryptedCompressedSecretAgent = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns1"]
          backup-policy  = "encryptedCompressedSecretAgentPolicy"
          secret-agent   = "absSecretAgent"
        }
        everySecondBackup = {
          backup-policy      = "defaultPolicy"
          source-cluster     = "absDefaultCluster"
          storage            = "backup-storage"
          interval-cron      = "* * * * * *"
          incr-interval-cron = ""
          namespaces = ["source-ns20"]
          disabled           = true
        }
        xdrBackup = {
          backup-policy  = "xdrBackup"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          interval-cron  = "@yearly"
          incr-interval-cron = "0 */1 * * * *" # every minute incremental backup
          namespaces = ["source-ns21"]
        }
        performanceTest = {
          interval-cron  = "@yearly"
          source-cluster = "absDefaultCluster"
          storage        = "backup-storage"
          namespaces = ["source-ns2"]
          backup-policy  = "defaultPolicy"
        }
      }
    }
  }
}