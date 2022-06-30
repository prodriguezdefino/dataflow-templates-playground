#!/bin/bash
set -xeu

if [ "$#" -ne 2 ] && [ "$#" -ne 3 ]
  then
    echo "Usage : sh create-template.sh <template file location> <gcp project> <gcp region>" 
    exit -1
fi

TEMPLATE_FILE=$1
GCP_PROJECT=$2

if [ "$#" -eq 2 ] 
  then
    GCP_REGION="us-central1"
  else
    GCP_REGION=$3
fi

export TEMPLATE_PATH=${TEMPLATE_FILE} 
export TEMPLATE_IMAGE="gcr.io/${GCP_PROJECT}/${GCP_REGION}/streamingpubsubtobq-template:latest"
  
gcloud auth configure-docker
# Build Docker Image
docker image build -t $TEMPLATE_IMAGE .
# Push image to Google Cloud Registry
docker push $TEMPLATE_IMAGE

gcloud dataflow flex-template build $TEMPLATE_PATH --image "$TEMPLATE_IMAGE" --sdk-language JAVA