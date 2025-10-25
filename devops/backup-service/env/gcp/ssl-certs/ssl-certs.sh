#!/bin/bash

set -e

PROJECT_ID="ecosystem-connectors-data"
REGION="me-west1"
BUCKET="ecosys-workspace-vars"
PREFIX="certbot"
FILE="certbot-output.tar.gz"
LOCAL_DEST="$PWD/$FILE"

ensure_fresh_secret() {
	local secret_id="$1"

	echo "Ensuring fresh secret: $secret_id"

	if ! gcloud secrets describe "$secret_id" --project="$PROJECT_ID" >/dev/null 2>&1; then
		echo "Secret $secret_id does not exist. Creating..."
		gcloud secrets create "$secret_id" \
			--replication-policy=user-managed \
			--locations="$REGION" \
			--project="$PROJECT_ID" \
			--labels="resource=letsencrypt" \
			--quiet
	else
		echo "Secret $secret_id exists. Destroying old versions..."
		for version in $(gcloud secrets versions list "$secret_id" \
			--project="$PROJECT_ID" \
			--format="value(name)" \
			--filter="state!=DESTROYED" | tail -n +2); do
			echo "Destroying version: $version"
			gcloud secrets versions destroy "$version" \
				--secret="$secret_id" \
				--project="$PROJECT_ID" \
				--quiet
		done
	fi
}

echo "Checking if archive exists in bucket..."

if gcloud storage objects list "gs://$BUCKET/$PREFIX" \
	--filter="name:$PREFIX/$FILE" \
	--project="$PROJECT_ID" \
	--format="value(name)" | grep -q "$PREFIX/$FILE"; then

	echo "File exists: gs://$BUCKET/$PREFIX/$FILE"
	echo "Downloading $FILE ..."
	gcloud storage cp "gs://$BUCKET/$PREFIX/$FILE" "$LOCAL_DEST" --project="$PROJECT_ID"

	echo "Extracting $LOCAL_DEST ..."
	tar -xzf "$LOCAL_DEST" -C "$PWD"
	echo "Extraction complete."

	echo "Removing local archive..."
	rm "$LOCAL_DEST"

else
	echo "File does not exist: gs://$BUCKET/$PREFIX/$FILE"
	echo "Creating fresh work directories..."
	mkdir -p "$PWD/workdir" "$PWD/logs" "$PWD/configdir"
fi

echo "Running Certbot to obtain/renew certificates..."
certbot certonly \
	--manual \
	--preferred-challenges dns \
	--manual-auth-hook "$PWD/auth-hook.sh" \
	--non-interactive \
	--agree-tos \
	--email dgerchikov@aerospike.com \
	-d ecoeng.dev \
	-d automation.ecoeng.dev \
	-d reportportal.ecoeng.dev \
	--config-dir "$PWD/configdir" \
	--work-dir "$PWD/workdir" \
	--logs-dir "$PWD/logs"

echo "Uploading fullchain.pem to Secret Manager..."
ensure_fresh_secret "fullchain-pem"
gcloud secrets versions add "fullchain-pem" \
	--project="$PROJECT_ID" \
	--data-file="$PWD/configdir/live/ecoeng.dev/fullchain.pem" \
	--quiet

echo "Uploading privkey.pem to Secret Manager..."
ensure_fresh_secret "privkey-pem"
gcloud secrets versions add "privkey-pem" \
	--project="$PROJECT_ID" \
	--data-file="$PWD/configdir/live/ecoeng.dev/privkey.pem" \
	--quiet

echo "Archiving workdir, logs, configdir to $LOCAL_DEST..."
tar -czf "$LOCAL_DEST" -C "$PWD" workdir logs configdir
echo "Archive created: $LOCAL_DEST"

echo "Uploading archive to GCS: gs://$BUCKET/$PREFIX/$FILE"
gcloud storage cp "$LOCAL_DEST" "gs://$BUCKET/$PREFIX/$FILE" --project="$PROJECT_ID"
echo "Upload complete."

echo "Cleaning up local archive and directories..."
rm "$LOCAL_DEST"
rm -rf "$PWD/workdir" "$PWD/logs" "$PWD/configdir"

echo "Done! All tasks completed successfully."
