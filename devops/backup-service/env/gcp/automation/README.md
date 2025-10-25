# Automation Server Deployment on GCP

## Prerequisites
- [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)
- [helm](https://helm.sh/docs/intro/install/)
- [gcloud](https://cloud.google.com/sdk/docs/install)
- [ansible](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html#installing-ansible)
- [terraform](https://developer.hashicorp.com/terraform/install?product_intent=terraform)
- [terragrunt](https://terragrunt.gruntwork.io/docs/getting-started/install/)
- [taskfile](https://taskfile.dev/installation/)
- [fzf](https://github.com/junegunn/fzf?tab=readme-ov-file#installation)

## Deploy Automation Server
```shell
task apply
```

## Destroy Automation Server
```shell
task destroy
```

## IAP tunneling
To reach report-portal api from your local machine, you can open IAP tunneling and use report-portal as it running locally (using `127.0.0.1`)
```shell
gcloud compute start-iap-tunnel \
--project="ecosystem-connectors-data" \
--zone=me-west1-a "$(gcloud compute instances list \
--project="ecosystem-connectors-data" \
--filter="name~'^ecosys'" \
--format="value(name)" \
--limit=1)" 8080 \
--local-host-port=localhost:8080
```

## `gcloud` IAP slowness
Please follow the steps below in case you experiencing slowness connecting IAP tunnel or when deploying the env - for more details please refer to [GCP docs](https://cloud.google.com/iap/docs/using-tcp-forwarding#increasing_the_tcp_upload_bandwidth).
```shell
echo "export CLOUDSDK_PYTHON_SITEPACKAGES=1" >> ~/.zshrc
$(gcloud info --format="value(basic.python_location)") -m pip install numpy
```
