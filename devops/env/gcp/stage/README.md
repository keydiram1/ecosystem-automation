
## Usefull Commands

## Create Custom Role for abs-testing-bucket
<<<<<<< Updated upstream

```shell
gcloud iam roles create absTestingBucketRole \
  --project="ecosystem-connectors-data" \
  --title="Role for abs-testing-bucket" \
  --description="Custom role for bucket object admin and listing buckets" \
  --permissions="storage.buckets.list" \
  --stage="GA"
```

## Add absTestingBucketRole to aerospike-backup-service-sa
```shell
gcloud projects add-iam-policy-binding "ecosystem-connectors-data" \
  --member="serviceAccount:aerospike-backup-service-sa@ecosystem-connectors-data.iam.gserviceaccount.com" \
  --role="projects/ecosystem-connectors-data/roles/absTestingBucketRole"
```

## Add objectAdmin role to aerospike-backup-service-sa
```shell
gcloud storage buckets add-iam-policy-binding gs://abs-testing-bucket \
  --member="serviceAccount:aerospike-backup-service-sa@ecosystem-connectors-data.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin" --project="ecosystem-connectors-data"
```

Create `roles/storage.legacyBucketOwner` on `abs-testing-bucket` for 
`aerospike-backup-service-sa@ecosystem-connectors-data.iam.gserviceaccount.com`:
=======
>>>>>>> Stashed changes

```shell
gcloud iam roles create absTestingBucketRole \
  --project="ecosystem-connectors-data" \
  --title="Role for abs-testing-bucket" \
  --description="Custom role for bucket object admin and listing buckets" \
  --permissions="storage.buckets.list" \
  --stage="GA"
```

## Add absTestingBucketRole to aerospike-backup-service-sa
```shell
gcloud projects add-iam-policy-binding "ecosystem-connectors-data" \
  --member="serviceAccount:aerospike-backup-service-sa@ecosystem-connectors-data.iam.gserviceaccount.com" \
  --role="projects/ecosystem-connectors-data/roles/absTestingBucketRole"
```

## Add objectAdmin role to aerospike-backup-service-sa
```shell
gcloud storage buckets add-iam-policy-binding gs://abs-testing-bucket \
  --member="serviceAccount:aerospike-backup-service-sa@ecosystem-connectors-data.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin" --project="ecosystem-connectors-data"
```

Create `features.conf` file secret on GCP:

```shell
gcloud secrets create "features-conf" \
  --project="ecosystem-connectors-data" \
  --replication-policy="user-managed" \
  --locations="me-west1" \
  --labels=environment=jenkins,jenkins-credentials-type=file,jenkins-credentials-filename=features,jenkins-credentials-file-extension=conf,resource=asdb

gcloud secrets versions add "features-conf" 
  --project="ecosystem-connectors-data" 
  --data-file="features.conf"
```

Create Service Account for Aerospike Secret Agent deployed locally
```shell
 gcloud iam service-accounts create aerospike-secret-agent-local \
 --project="ecosystem-connectors-data" \
 --description="Arospike Secret Agent SA for local deployment" \
 --display-name="aerospike-secret-agent-local-sa"
```
Add Secret Manager Secret Accessor role
```shell
gcloud projects add-iam-policy-binding ecosystem-connectors-data \
    --member="serviceAccount:aerospike-secret-agent-local@ecosystem-connectors-data.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
```

Create Service Account aerospike-secret-agent-local-sa.json for Aerospike Secret Agent deployed locally
```shell
gcloud iam service-accounts keys create aerospike-secret-agent-local-sa.json \
 --project="ecosystem-connectors-data" \
--iam-account=aerospike-secret-agent-local@ecosystem-connectors-data.iam.gserviceaccount.com
```

Create secret for aerospike-secret-agent-local-sa.json to GCP secret manager
```shell
gcloud secrets create "aerospike-secret-agent-local-sa" \
  --project="ecosystem-connectors-data" \
  --replication-policy="user-managed" \
  --locations="me-west1" \
  --labels=environment=jenkins,jenkins-credentials-type=file,jenkins-credentials-filename=aerospike-secret-agent-local-sa,jenkins-credentials-file-extension=json,resource=local-deployment
```

Upload aerospike-secret-agent-local-sa.json to created secret
```shell
gcloud secrets versions add "aerospike-secret-agent-local-sa"  \
--project="ecosystem-connectors-data" \
--data-file="aerospike-secret-agent-local-sa.json" 
```

