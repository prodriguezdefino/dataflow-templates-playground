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

PIPELINE_NAME=streamingpubsubtobq
BUILD_TAG=$(date +"%Y-%m-%d_%H-%M-%S")

export TEMPLATE_PATH=${TEMPLATE_FILE} 
export TEMPLATE_IMAGE="gcr.io/${GCP_PROJECT}/${GCP_REGION}/${PIPELINE_NAME}-template:latest"

GCS_TEMPLATE_PATH=gs://${BUCKET}/template/streamingpubsubtobq-template.json  

gcloud auth configure-docker
# Build Docker Image
docker image build -t $TEMPLATE_IMAGE .
# Push image to Google Cloud Registry
docker push $TEMPLATE_IMAGE

gcloud dataflow flex-template build $GCS_TEMPLATE_PATH \
  --image "$TEMPLATE_IMAGE" \
  --sdk-language JAVA \
  --metadata-file "${TEMPLATE_FILE}" \
  --additional-user-labels template-name=${PIPELINE_NAME},template-version=${BUILD_TAG} \
  --additional-experiments enable_recommendations,enable_google_cloud_profiler,enable_google_cloud_heap_sampling 
