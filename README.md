## Prerequisites 

List of preinstalled software needed in the environment to create this template: 
* gcloud
* docker (if using mac os consider installing using `brew install colima && colima start`)

## Create the Dataflow Template

Execute the included script, run `sh create-template.sh` for example usage

## Launch the Dataflow Template

Execute the included script, run `sh launch-template.sh` for example usage

## Launch the Streaming Data Generator

Execute the `launch_datagen_templates.sh` with something like: 
```
./launch_datagen_templates.sh $PROJECT $SCHEMA_FILENAME $SCHEMA_LOCATION $QPS $TOPIC $PREFIX_NAME "<list of GCP regions>"
```