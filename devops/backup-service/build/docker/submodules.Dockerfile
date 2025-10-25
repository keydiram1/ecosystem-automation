FROM aerospike.jfrog.io/ecosystem-container-dev-local/abs-base-image:latest as builder

ARG GIT_BRANCH
ARG GIT_COMMIT_SHA
ARG VERSION
ARG ISO8601

WORKDIR /app
ENV PATH="${PATH}:/usr/local/go/bin"

RUN <<-EOF
  git clone https://github.com/aerospike/aerospike-backup-service.git
  cd /app/aerospike-backup-service
  git checkout "${GIT_BRANCH}"
  git submodule update --init --recursive
  cd /app
  make -C /app/aerospike-backup-service prep-submodules
  make -C /app/aerospike-backup-service build-submodules
EOF
