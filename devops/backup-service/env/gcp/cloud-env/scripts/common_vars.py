#!/usr/bin/env python3
import os
import yaml
import argparse
from pathlib import Path
from google.cloud import storage

PARENT_DIR = Path(Path(__file__).resolve().parent).parent
PARENT_DIR_NAME = PARENT_DIR.name
COMMON_VARS_FILENAME = "common_vars.yaml"
COMMON_VARS = os.environ.get("COMMON_VARS", os.path.join(PARENT_DIR, COMMON_VARS_FILENAME))
BUCKET_NAME = os.environ.get("BUCKET_NAME", "ecosys-workspace-vars")
PROJECT_ID = os.environ.get("PROJECT_ID", "ecosystem-connectors-data")
REGION = os.environ.get("REGION", "me-west1")
ZONE = os.environ.get("ZONE", "me-west1-a")


def upload_common_vars():
    with open(COMMON_VARS, "r") as file:
        common_vars = yaml.safe_load(file)

    storage_client = storage.Client(project=PROJECT_ID)
    bucket = storage_client.bucket(BUCKET_NAME)

    if not bucket.exists():
        bucket.create(location=REGION, storage_class="STANDARD")

    common_vars_blob = bucket.blob(os.path.join(PARENT_DIR_NAME, COMMON_VARS_FILENAME))
    common_vars_blob.upload_from_string(yaml.dump(common_vars, default_flow_style=True))
    common_vars_blob.update_storage_class("STANDARD")


def download_common_vars():
    storage_client = storage.Client(project=PROJECT_ID)
    bucket = storage_client.bucket(BUCKET_NAME)

    common_vars_blob = bucket.blob(os.path.join(PARENT_DIR_NAME, COMMON_VARS_FILENAME))

    if common_vars_blob.exists():
        common_vars_blob.download_to_filename(os.path.join(PARENT_DIR, COMMON_VARS_FILENAME))


def main():
    parser = argparse.ArgumentParser(prog="common-vars")
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--upload", action="store_true", help="Upload common-vars")
    group.add_argument("--download", action="store_true", help="Download common-vars")

    args = parser.parse_args()

    if args.upload:
        upload_common_vars()

    if args.download:
        download_common_vars()


if __name__ == "__main__":
    main()
