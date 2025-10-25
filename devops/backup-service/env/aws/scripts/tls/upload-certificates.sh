#!/bin/bash -e

DIRECTORY="$(pwd)/rootca/output"
if [ ! -d "$DIRECTORY" ] || [ ! "$(ls -A $DIRECTORY)" ]; then
   exit 1
fi

json="{}"
for file in "$DIRECTORY"/*; do

    encoded_content=$(base64 -w0 "$file")
    filename=$(basename "$file" | tr '.' '_')
    json=$(echo "$json" | jq --arg key "$filename" --arg value "$encoded_content" '. + {($key): $value}')
done

aws secretsmanager create-secret \
--name testenv \
--secret-string "$json" \
--tags Key=Name,Value="testenv" Key=Project,Value=abs
