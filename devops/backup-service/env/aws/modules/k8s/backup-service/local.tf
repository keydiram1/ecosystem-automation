locals {
  dns_name         = "${replace(var.prefix, "-", ".")}.internal"
  service_name     = "backup-service"
  gateway_dns_name = "gateway.${local.dns_name}"
  secret_agent_svc = "secret-agent.aerospike.svc.cluster.local"
  config_path      = "config/aerospike-backup-service.yml"

  service_config = {
    service = {
      logger = {
        level = "INFO"
      }
    }
    aerospike-clusters = {
      absDefaultCluster = {
        label      = "server.abs"
        seed-nodes = [
          {
            host-name = "${var.cluster_name}.${local.dns_name}"
            port      = 4333
            tls-name  = "server.abs"
          }
        ]
        credentials = {
          user          = "tester"
          password-path = "/config/psw.txt"
        }
        tls = {
          name   = "server.abs"
           ca-file = "/etc/aerospike-backup-service/ssl/rootCA.pem"
        }
        max-parallel-scans = 20
      }
    }
    secret-agents = {
      absSecretAgent = {
        connection-type = "tcp"
        address     = local.secret_agent_svc
        port        = 3005
        is-base64 = true
      }
    }

    storage = {
      awsStorage = {
        s3-storage = {
          bucket               = var.s3.name
          path                 = "awsStorage"
          s3-region            = data.aws_region.current.name
        }
      }
    }

    backup-policies = {
      encryptedCompressedSecretAgentPolicy = {
        parallel     = 8
        retention = {
          full = 1
          incremental = 0
        }
        sealed       = true
        encryption   = {
          key-secret = "secrets:TestEnvTls:encryption-key-pem"
          mode: "AES256"
        }
        compression = {
          level = 20
          mode = "ZSTD"
        }
      }
      defaultPolicy = {
        parallel     = 8
        sealed       = true
        with-cluster-configuration = true
      }
    backup-routines = {
      minio = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns4"]
        backup-policy  = "defaultPolicy"
      }
      edgeCases = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns7"]
        backup-policy  = "defaultPolicy"
      }
      filterBySetAndBin = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns8"]
        set-list       = ["backupSet"]
        bin-list       = ["backupBin"]
        backup-policy  = "defaultPolicy"
      }
      localStorageIncremental3 = {
        interval-cron      = "@yearly"
        incr-interval-cron = "*/10 * * * * *"
        source-cluster     = "absDefaultCluster"
        storage            = "awsStorage"
        namespaces         = ["source-ns10"]
        backup-policy      = "defaultPolicy"
      }
      fullBackup1 = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns9"]
        backup-policy  = "defaultPolicy"
      }
      fullBackup2 = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns11"]
        backup-policy  = "defaultPolicy"
      }
      fullBackup3 = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns12"]
        backup-policy  = "defaultPolicy"
      }
      timestamp = {
        name               = "timestamp"
        interval-cron      = "@yearly"
        incr-interval-cron = "*/9 * * * * *"
        source-cluster     = "absDefaultCluster"
        storage            = "awsStorage"
        namespaces         = ["source-ns14"]
        backup-policy      = "defaultPolicy"
      }
      incrementalBackupCluster = {
        interval-cron      = "@yearly"
        incr-interval-cron = "*/45 * * * * *"
        source-cluster     = "absDefaultCluster"
        storage            = "awsStorage"
        namespaces         = []
        backup-policy      = "defaultPolicy"
      }
      incrementalBackupMultipleNSs = {
        interval-cron      = "@yearly"
        incr-interval-cron = "*/30 * * * * *"
        source-cluster     = "absDefaultCluster"
        storage            = "awsStorage"
        namespaces         = ["source-ns18", "source-ns19", "source-ns20"]
        backup-policy      = "defaultPolicy"
      }
      fullBackupFullCluster = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = []
        backup-policy  = "defaultPolicy"
      }
      fullBackup3Namespaces = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns15", "source-ns16", "source-ns17"]
        backup-policy  = "defaultPolicy"
      }
      fullBackup4 = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns21"]
        backup-policy  = "defaultPolicy"
      }
      fullBackup5 = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns22"]
        backup-policy  = "defaultPolicy"
      }
      fullBackup6 = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns23"]
        backup-policy  = "defaultPolicy"
      }
      fullBackupEncryptedCompressedSecretAgent = {
        interval-cron  = "@yearly"
        source-cluster = "absDefaultCluster"
        storage        = "awsStorage"
        namespaces     = ["source-ns1"]
        backup-policy  = "encryptedCompressedSecretAgentPolicy"
        secret-agent = "absSecretAgent"
      }
      everySecondBackup = {
        backup-policy     = "defaultPolicy"
        source-cluster    = "absDefaultCluster"
        storage           = "backup-storage"
        interval-cron     = "* * * * * *"
        incr-interval-cron = ""
        namespaces        = ["source-ns20"]
        disabled          = true
      }
    }
  }

  remote_config = {
    s3-storage = {
      bucket     = var.s3.name
      path       = local.config_path
      s3-region  = data.aws_region.current.name
    }
  }
}