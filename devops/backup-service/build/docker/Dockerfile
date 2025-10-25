FROM registry.access.redhat.com/ubi9/ubi:latest as builder

ARG CACHEBUST=1
ARG GIT_BRANCH
ARG GO_VERSION=1.22.0
ARG TARGETARCH
ARG GIT_COMMIT_SHA
ARG VERSION
ARG ISO8601

# Install Go
RUN <<-EOF
  set -e
  curl -Lo /tmp/go.tgz "https://go.dev/dl/go${GO_VERSION}.linux-${TARGETARCH}.tar.gz"
  tar -xzf /tmp/go.tgz -C /usr/local/
  rm /tmp/go.tgz
EOF

ENV PATH="/usr/local/go/bin:${PATH}"
ENV GOPRIVATE=github.com/aerospike/backup-go
WORKDIR /app

# Install necessary packages
RUN dnf -y install git make cmake automake gcc gcc-c++ && dnf clean all

# Configure git to use the GitHub token for authentication
RUN --mount=type=secret,id=GITHUB_TOKEN \
    git config --global credential.helper store && \
    echo "https://$(cat /run/secrets/GITHUB_TOKEN):@github.com" > ~/.git-credentials

RUN echo "GIT_BRANCH: ${GIT_BRANCH}"

# Clone the repository and build
RUN <<-EOF
  set -e
  git clone -b "${GIT_BRANCH}" --single-branch https://github.com/aerospike/aerospike-backup-service.git
  cd /app/aerospike-backup-service
  go get github.com/aerospike/backup-go@main
  git submodule init
  git submodule update --recursive --remote
  go get github.com/aerospike/aerospike-management-lib@v1.4.0
  go get github.com/go-openapi/spec@v0.20.6
  make build
EOF

FROM registry.access.redhat.com/ubi9/ubi-minimal:latest

ARG GIT_BRANCH
ARG GIT_COMMIT_SHA
ARG VERSION
ARG ISO8601

LABEL org.opencontainers.image.title="Aerospike Backup Service" \
      org.opencontainers.image.description="Aerospike Backup Service provides a set of REST APIs to schedule full and incremental backups. Additionally, these APIs can be used to restore data from a backup to a cluster" \
      org.opencontainers.image.documentation="https://github.com/aerospike/aerospike-backup-service?tab=readme-ov-file#aerospike-backup-service" \
      org.opencontainers.image.base.name="registry.access.redhat.com/ubi9/ubi-minimal" \
      org.opencontainers.image.source="https://github.com/aerospike/aerospike-backup-service/tree/${GIT_BRANCH}" \
      org.opencontainers.image.vendor="Aerospike" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.url="https://github.com/aerospike/aerospike-backup-service" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.revision="${GIT_COMMIT_SHA}" \
      org.opencontainers.image.created="${ISO8601}"

COPY --from=builder /app/aerospike-backup-service/target/aerospike-backup-service /usr/bin/aerospike-backup-service
COPY --from=builder /app/aerospike-backup-service/config/config.yml /etc/aerospike-backup-service/aerospike-backup-service.yml

EXPOSE 8080
ENV DOCKER_CONTAINER=true

ENTRYPOINT ["aerospike-backup-service"]
CMD ["-c", "/etc/aerospike-backup-service/aerospike-backup-service.yml"]