#!/bin/bash

USERNAME="ubuntu"
HOST="${@: -2:1}"
CMD="${@: -1:1}"

declare -a OPTS
for SCP_ARG in "${@:1:$#-3}"; do
	if [[ "${SCP_ARG}" == --* ]]; then
		OPTS+="${SCP_ARG} "
	fi
done

CMD=$(echo "${CMD}" | tr -d [])

exec gcloud compute scp $OPTS "${USERNAME}@${HOST}" "${CMD}"
