pipeline {
    agent {
        dockerContainer {
            image 'eu.gcr.io/google.com/cloudsdktool/google-cloud-cli:debian_component_based'
        }
    }
    triggers {
        cron("H */4 * * *")
    }
    environment {
        SECRET_ID = "rp-token"
        PROJECT_ID = "ecosystem-connectors-data"
        REGION = "me-west1"
        RP_DNS = "rp.ecosys.internal"
    }
    stages {
        stage("Run RP renewal script") {
            steps {
                withCredentials([file(credentialsId: "gcloud-creds", variable: "GCLOUD_CREDS")]) {
                    sh '''#!/bin/bash
                            gcloud auth activate-service-account --project="$PROJECT_ID" --key-file="$GCLOUD_CREDS"

                            generate_secret() {
                                TOKEN_RESPONSE="$(curl --silent --show-error --fail \
                                --header "Content-Type: application/x-www-form-urlencoded" \
                                --request POST \
                                --data "grant_type=password&username=superadmin&password=erebus" \
                                --user "ui:uiman" http://$RP_DNS:8080/uat/sso/oauth/token)"

                                if [ $? -ne 0 ]; then
                                    echo "Error: Failed to retrieve token"
                                    exit 1
                                fi

                                echo -n "$TOKEN_RESPONSE" | gcloud secrets versions add "$SECRET_ID" --project="$PROJECT_ID" --data-file=-
                            }
                            if ! gcloud secrets describe "$SECRET_ID" --project="$PROJECT_ID" >/dev/null 2>&1; then
                                gcloud secrets create "$SECRET_ID" \
                                --replication-policy=user-managed \
                                --locations="$REGION" \
                                --project="$PROJECT_ID" \
                                --labels="jenkins-credentials-type=string,environment=jenkins" \
                                --quiet
                            fi

                            if ! gcloud secrets versions list "$SECRET_ID" --project="$PROJECT_ID" --quiet --format="value(name)" | grep -q .; then
                              generate_secret
                            else
                                TIME_DIFF="$(("$(date -u +"%s")" - \
                                $(date -d "$(gcloud secrets versions list "$SECRET_ID" \
                                --project="$PROJECT_ID" --format="value(createTime)" --limit=1)" +"%s") ))"

                                if [ "$TIME_DIFF" -gt 72000 ]; then
                                    for VERSION in $(gcloud secrets versions list "$SECRET_ID" \
                                      --project="$PROJECT_ID" \
                                      --format="value(name)" \
                                      --filter="state!=DESTROYED" | tail -n +2); do
                                        gcloud secrets versions destroy "$VERSION" --secret="$SECRET_ID" --project="$PROJECT_ID" --quiet
                                    done
                                    generate_secret
                                fi
                            fi
                    '''
                }
            }
        }
    }
}
