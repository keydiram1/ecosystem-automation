#!/bin/bash
aws s3api create-bucket --bucket abs-testing-bucket --region il-central-1 --create-bucket-configuration LocationConstraint=il-central-1

aws s3api put-bucket-lifecycle-configuration --bucket abs-testing-bucket --lifecycle-configuration file://<(cat <<EOF
{
  "Rules": [
    {
      "ID": "Delete after 7 days",
      "Filter": {
        "Prefix": ""
      },
      "Status": "Enabled",
      "Expiration": {
        "Days": 7
      }
    }
  ]
}
EOF
)


aws s3api put-bucket-policy --bucket abs-testing-bucket --policy file://<(cat <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "accounts.google.com"
      },
      "Action": [
        "s3:GetObject",
        "s3:ListBucket",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::abs-testing-bucket",
        "arn:aws:s3:::abs-testing-bucket/*"
      ],
      "Condition": {
        "StringEquals": {
          "accounts.google.com:sub": "test-asa-sa@ecosystem-connectors-data.iam.gserviceaccount.com"
        }
      }
    },
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "accounts.google.com"
      },
      "Action": [
        "s3:ListAllMyBuckets",
        "s3:GetBucketLocation"
      ],
      "Resource": "arn:aws:s3:::*",
      "Condition": {
        "StringEquals": {
          "accounts.google.com:sub": "test-asa-sa@ecosystem-connectors-data.iam.gserviceaccount.com"
        }
      }
    }
  ]
}
EOF
)
