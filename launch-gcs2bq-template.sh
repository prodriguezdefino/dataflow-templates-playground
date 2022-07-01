#!/bin/bash
set -xeu

if [ "$#" -ne 4 ] && [ "$#" -ne 5 ]
  then
    echo "Usage : sh create-template.sh <gcp project> <template gcs location> <input> <output> <optional parameters>" 
    exit -1
fi

GCP_PROJECT=$1
TEMPLATE_LOCATION=$2
INPUT=$3
OUTPUT=$4


gcloud dataflow flex-template run "process-gcstobq-`date +%Y%m%d-%H%M%S`" \
    --template-file-gcs-location "${TEMPLATE_LOCATION}" \
    --project "${GCP_PROJECT}" \
    --parameters source=GCS \
    --parameters inputURL="${INPUT}" \
    --parameters destination=BIGQUERY \
    --parameters outputURL="${OUTPUT}" \
    --parameters UDFClassName="com.google.cloud.pso.dataflow.udf.CustomUDF" \
    --region "us-central1"
