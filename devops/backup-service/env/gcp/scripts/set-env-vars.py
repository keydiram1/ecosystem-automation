#!/usr/bin/env python3
import os
import yaml
import argparse
from pathlib import Path
from google.cloud import storage

PARENT_DIR = Path(Path(__file__).resolve().parent).parent
ENV_VARS_PROPERTIES = os.path.join(os.getcwd(), "env.properties")
COMMON_VARS_FILENAME = "common_vars.yaml"
BUCKET_NAME = os.environ.get("BUCKET_NAME", "ecosys-workspace-vars")
PROJECT_ID = os.environ.get("PROJECT_ID", "ecosystem-connectors-data")


def set_env(workspace):
    storage_client = storage.Client(project=PROJECT_ID)
    bucket = storage_client.bucket(BUCKET_NAME)

    asdb_common_vars_blob = bucket.blob(os.path.join(workspace, "asdb", COMMON_VARS_FILENAME))

    if not asdb_common_vars_blob.exists():
        raise FileNotFoundError(f"asdb {COMMON_VARS_FILENAME} does not exists on the bucket")

    abs_common_vars_blob = bucket.blob(os.path.join(workspace, "abs", COMMON_VARS_FILENAME))

    if not abs_common_vars_blob.exists():
        raise FileNotFoundError(f"asdb {COMMON_VARS_FILENAME} does not exists on the bucket")

    if not abs_common_vars_blob.exists():
        raise FileNotFoundError(f"abs {COMMON_VARS_FILENAME} does not exists on the bucket")

    asdb_common_vars = yaml.safe_load(asdb_common_vars_blob.download_as_bytes().decode("utf-8"))
    abs_common_vars = yaml.safe_load(abs_common_vars_blob.download_as_bytes().decode("utf-8"))

    data = {
        "ABS_STORAGE_PROVIDER": abs_common_vars["k8s"]["abs"]['storage-provider'],
        "ABS_IMAGE_TAG": abs_common_vars["k8s"]["abs"]["image-tag"],
        "ASDB_VERSION": asdb_common_vars["asdb"]["version"]
    }
    print("writing env vars to env.properties")
    with open(ENV_VARS_PROPERTIES, "w") as f:
        for key, value in data.items():
            f.write(f"{key}={value}\n")


def main():
    parser = argparse.ArgumentParser(prog="set-env")
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--workspace", type=str)

    args = parser.parse_args()

    if args.workspace:
        set_env(workspace=args.workspace)


if __name__ == "__main__":
    main()
