#!/usr/bin/env python3
import os
import json
import yaml
import argparse
import subprocess
from pathlib import Path
from google.cloud import storage


PARENT_DIR = Path(Path(__file__).resolve().parent).parent
ENV_VARS_PROPERTIES = os.path.join(PARENT_DIR, "env.properties")
COMMON_VARS_FILENAME = "common_vars.yaml"
METADATA_FILENAME = "metadata.yaml"
ASDB_DIR = os.path.join(PARENT_DIR, "live", "asdb")
GW_DIR = os.path.join(PARENT_DIR, "live", "k8s", "gateway")
COMMON_VARS = os.environ.get("COMMON_VARS", os.path.join(PARENT_DIR, COMMON_VARS_FILENAME))
BUCKET_NAME = os.environ.get("BUCKET_NAME", "ecosys-workspace-vars")
PROJECT_ID = os.environ.get("PROJECT_ID", "ecosystem-connectors-data")
REGION = os.environ.get("REGION", "me-west1")
ZONE = os.environ.get("ZONE", "me-west1-a")

ASDB_IPS_CMD = f"terragrunt --working-dir {ASDB_DIR} output -terragrunt-log-level stderr -json asdb_node_ips"
GW_DNS_CMD = f"terragrunt --working-dir {GW_DIR} output -terragrunt-log-level stderr -json gateway_ip"


def upload_metadata():

    with open(COMMON_VARS, "r") as file:
        common_vars = yaml.safe_load(file)

    asdb_ips_output = subprocess.check_output(ASDB_IPS_CMD.split(), text=True)
    asdb_ips = json.loads(asdb_ips_output)
    gw_dns_output = subprocess.check_output(GW_DNS_CMD.split(), text=True)
    metadata = {"asdb-ips": asdb_ips, "gw-ip": gw_dns_output.strip().strip('"')}

    storage_client = storage.Client(project=PROJECT_ID)
    bucket = storage_client.bucket(BUCKET_NAME)

    if not bucket.exists():
        bucket.create(location=REGION, storage_class="STANDARD")

    workspace = common_vars["workspace"]
    common_vars_blob = bucket.blob(os.path.join(workspace, COMMON_VARS_FILENAME))
    common_vars_blob.upload_from_string(yaml.dump(common_vars, default_flow_style=True))
    common_vars_blob.update_storage_class("STANDARD")

    metadata_blob = bucket.blob(os.path.join(workspace, METADATA_FILENAME))
    metadata_blob.upload_from_string(yaml.dump(metadata, default_flow_style=True))
    metadata_blob.update_storage_class("STANDARD")

def download_metadata(workspace):
    storage_client = storage.Client(project=PROJECT_ID)
    bucket = storage_client.bucket(BUCKET_NAME)

    common_vars_blob = bucket.blob(os.path.join(workspace, COMMON_VARS_FILENAME))

    if common_vars_blob.exists():
        common_vars_blob.download_to_filename(os.path.join(PARENT_DIR, COMMON_VARS_FILENAME))

    metadata_blob = bucket.blob(os.path.join(workspace, METADATA_FILENAME))

    if metadata_blob.exists():
        metadata_blob.download_to_filename(os.path.join(PARENT_DIR, METADATA_FILENAME))

def set_env(workspace):
    storage_client = storage.Client(project=PROJECT_ID)
    bucket = storage_client.bucket(BUCKET_NAME)

    common_vars_blob = bucket.blob(os.path.join(workspace, COMMON_VARS_FILENAME))

    if not common_vars_blob.exists():
        raise FileNotFoundError(f"{COMMON_VARS_FILENAME} does not exists on the bucket")

    common_vars = yaml.safe_load(common_vars_blob.download_as_bytes().decode("utf-8"))

    metadata_blob = bucket.blob(os.path.join(workspace, METADATA_FILENAME))

    if not metadata_blob.exists():
        raise FileNotFoundError(f"{METADATA_FILENAME} does not exists on the bucket")

    metadata = yaml.safe_load(metadata_blob.download_as_bytes().decode("utf-8"))

    data = {
        "ASDB_IPS": metadata["asdb-ips"],
        "GW_IP": metadata["gw-ip"],
        "STORAGE_PROVIDER": common_vars["storage"]["provider"],
        "ABS_VERSION": common_vars["k8s"]["abs"]["version"],
        "ASDB_VERSION": common_vars["asdb"]["version"]
    }
    print(f"Data: {data}")

    with open(ENV_VARS_PROPERTIES, "w") as f:
        for key, value in data.items():
            f.write(f"{key}={value}\n")


def main():
    parser = argparse.ArgumentParser(prog="metadata")
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--upload", action="store_true", help="Upload metadata")
    group.add_argument("--download", type=str)
    group.add_argument("--set-env", type=str)

    args = parser.parse_args()

    if args.upload:
        upload_metadata()

    if args.download is None and args.set_env == "":
        raise ValueError("Workspace is not set")

    if args.download == "" and args.set_env is None:
        raise ValueError("Workspace is not set")

    if args.download:
        download_metadata(workspace=args.download)

    if args.set_env:
        set_env(workspace=args.set_env)


if __name__ == "__main__":
    main()
