#!/bin/bash
PROJECT_ID="ecosystem-connectors-data"
REGION="me-west1"

WORKER_SSH_KEY_PAIR_DIR="$(pwd)/ssh-key-pair"
WORKER_SSH_KEY_PAIR_NAME="worker-ssh-key-pair"
WORKER_USER="ubuntu"

mkdir -p "$WORKER_SSH_KEY_PAIR_DIR"
ssh-keygen -t rsa -f "$WORKER_SSH_KEY_PAIR_DIR/$WORKER_SSH_KEY_PAIR_NAME" -C "$WORKER_USER" -N ""

ensure_fresh_secret() {

	local secret_id="$1"

	if ! gcloud secrets describe "$secret_id" --project="$PROJECT_ID" >/dev/null 2>&1; then
		gcloud secrets create "$secret_id" \
			--replication-policy=user-managed \
			--locations="$REGION" \
			--project="$PROJECT_ID" \
			--labels="jenkins-credentials-type=file,jenkins-credentials-filename=$secret_id,jenkins-credentials-file-extension=pem,environment=jenkins,resource=worker" \
			--quiet
	else
		for version in $(gcloud secrets versions list "$secret_id" \
			--project="$PROJECT_ID" \
			--format="value(name)" \
			--filter="state!=DESTROYED" | tail -n +2); do
			gcloud secrets versions destroy "$version" --secret="$secret_id" --project="$PROJECT_ID" --quiet
		done
	fi
}

for file in "$WORKER_SSH_KEY_PAIR_DIR"/*; do
	if [[ -f "$file" ]]; then
		if [[ "$file" == *.pub ]]; then
			secret_id="$(basename "${file%.pub}")-pub"
		else
			secret_id="$(basename "$file")"
		fi

		ensure_fresh_secret "$secret_id"

		gcloud secrets versions add "$secret_id" \
			--project="$PROJECT_ID" \
			--data-file="$file" \
			--quiet
	fi
done
