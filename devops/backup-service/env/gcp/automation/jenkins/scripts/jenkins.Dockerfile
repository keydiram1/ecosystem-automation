# syntax=docker/dockerfile:1.12
FROM jenkins/jenkins:latest-jdk21
USER root

RUN <<-EOF
  apt-get update && apt-get install -y lsb-release
  curl -fsSLo /usr/share/keyrings/docker-archive-keyring.asc \
  https://download.docker.com/linux/debian/gpg
  echo "deb [arch="$(dpkg --print-architecture)" \
  signed-by=/usr/share/keyrings/docker-archive-keyring.asc] \
  https://download.docker.com/linux/debian "$(lsb_release -cs)" stable" > /etc/apt/sources.list.d/docker.list
  apt-get update && apt-get install -y docker-ce-cli sqlite3 && rm -rf /var/lib/apt/lists/*
EOF

USER jenkins

COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt

RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

HEALTHCHECK --start-period=10s --interval=10s --retries=3 --timeout=10s \
CMD curl -f http://localhost:8080/jenkins/login || exit 1
