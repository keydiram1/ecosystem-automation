#mkdir -p "$PWD/data/jenkins-data"
#cp ../../modules/jenkins/ansible/assets/jcasc.yaml jcasc.yaml
#
#if [ ! -f "$HOME/key-file.json" ]; then
#  gcloud iam service-accounts keys create "$HOME/key-file.json" \
#  --project="ecosystem-connectors-data" \
#  --iam-account="davidev@ecosystem-connectors-data.iam.gserviceaccount.com"
#fi

docker run \
	--rm \
	--user=jenkins \
	--name=ecosys-jenkins \
	--publish="8080:8080" \
	--publish="5005:5000" \
	--env="PROJECT_ID=ecosystem-connectors-data" \
	--env="ZONE=me-west1-a" \
	--env="REGION=me-west1" \
	--env="CASC_JENKINS_CONFIG=/usr/share/jenkins" \
	--env="GITHUB_TOKEN=$GITHUB_TOKEN" \
	--volume "$PWD/jcasc.yaml:/usr/share/jenkins/jenkins.yaml:ro" \
	--volume "jenkins-data:/var/jenkins_home" \
	--volume "/var/run/docker.sock:/var/run/docker.sock" \
	--volume "$PWD/docker.conf:/etc/default/docker:ro" \
	--net host \
	aerospike.jfrog.io/ecosystem-container-dev-local/ecosys-jenkins-server:1.0.0 "--prefix=/jenkins"
#--volume ":/usr/bin/docker" \

#--volume "/var/run/docker.sock:/var/run/docker.sock" \
#--volume "$(which docker):/usr/bin/docker" \
#--env="SA_KEY=$(cat "$HOME/key-file.json" |  jq -c | tr -d '\n' | base64 -w0)" \
