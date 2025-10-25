pipeline {
    agent {
        dockerContainer {
            image "eu.gcr.io/google.com/cloudsdktool/google-cloud-cli:debian_component_based"
        }
    }
    environment {
        SECRET_ID = "jenkins-token"
        PROJECT_ID = "ecosystem-connectors-data"
        REGION = "me-west1"
        JENKINS_DNS = "rp.ecosys.internal"
    }
    stages {
        stage() {
            steps {
                sh '''#!/bin/bash

                        CRUMB="$(curl "http://$JENKINS_DNS:8080/jenkins/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,%22:%22,//crumb)" \
                        --retry 3 \
                        --retry-delay 1 \
                        --silent \
                        --cookie-jar /tmp/cookies \
                        --user 'jenkins:jenkins')"

                        JENKINS_TOKEN="$(curl "http://$JENKINS_DNS:8080/jenkins/user/jenkins/descriptorByName/jenkins.security.ApiTokenProperty/generateNewToken" \
                        --user "jenkins:jenkins" \
                        --silent \
                        --data "newTokenName=api-token" \
                        --cookie /tmp/cookies \
                        --header "$CRUMB")"

                        gcloud auth activate-service-account --project="$PROJECT_ID" --key-file="$GCLOUD_CREDS"

                        if ! gcloud secrets describe "$SECRET_ID" --project="$PROJECT_ID" >/dev/null 2>&1; then

                            gcloud secrets create "$SECRET_ID" \
                            --replication-policy=user-managed \
                            --locations="$REGION" \
                            --project="$PROJECT_ID" \
                            --labels="jenkins-credentials-type=string,environment=jenkins" \
                            --quiet
                        else

                            for VERSION in $(gcloud secrets versions list "$SECRET_ID" \
                              --project="$PROJECT_ID" \
                              --format="value(name)" \
                              --filter="state!=DESTROYED" | \
                              tail -n +2); do
                                gcloud secrets versions destroy "$VERSION" --secret="$SECRET_ID" --project="$PROJECT_ID" --quiet
                            done
                        fi

                        echo -n "$JENKINS_TOKEN" | gcloud secrets versions add "$SECRET_ID" --project="$PROJECT_ID" --data-file=-
                '''
            }
        }
    }
}
