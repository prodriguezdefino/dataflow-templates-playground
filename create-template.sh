#!/bin/bash
set -xeu

if [ "$#" -ne 3 ] && [ "$#" -ne 4 ]
  then
    echo "Usage : sh create-template.sh <local template file location> <gcp project> <gcs bucket name> <gcp region>" 
    exit -1
fi

TEMPLATE_FILE=$1
GCP_PROJECT=$2
BUCKET=$3

if [ "$#" -eq 3 ] 
  then
    GCP_REGION="us-central1"
  else
    GCP_REGION=$4
fi

export TEMPLATE_PATH=${TEMPLATE_FILE} 
export TEMPLATE_IMAGE="gcr.io/${GCP_PROJECT}/${GCP_REGION}/streamingpubsubtobq-template:latest"

GCS_TEMPLATE_PATH=gs://${BUCKET}/template/streamingpubsubtobq-template.json
  
gcloud auth configure-docker
# Build Docker Image
docker image build -t $TEMPLATE_IMAGE .
# Push image to Google Cloud Registry
docker push $TEMPLATE_IMAGE

# copy the template file to the gcs location
gsutil cp $TEMPLATE_PATH GCS_TEMPLATE_PATH

gcloud dataflow flex-template build $GCS_TEMPLATE_PATH --image "$TEMPLATE_IMAGE" --sdk-language JAVA
