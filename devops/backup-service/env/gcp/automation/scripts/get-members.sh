#!/bin/bash
gcloud projects get-iam-policy "$1" \
	--flatten='bindings[].members' \
	--format='json(bindings.members)' |
	jq -c '[.[].bindings | select(.members | contains("user:")).members]'
