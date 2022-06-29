FROM adoptopenjdk/maven-openjdk11:latest AS build
ADD . /pipeline
WORKDIR /pipeline
USER root
RUN mvn clean package -Dmaven.test.skip=true

#
# Package stage
#
FROM gcr.io/dataflow-templates-base/java11-template-launcher-base:latest

ARG WORKDIR=/template
ARG MAINCLASS=com.example.dataflow.pubsub2pubsub.Pubsub2Pubsub
RUN mkdir -p ${WORKDIR}
WORKDIR ${WORKDIR}

# Bring the bundled jar file for the pipeline from the build container
COPY --from=build /pipeline/target/dataflow-pipeline-1.0-SNAPSHOT.jar /template/
RUN mkdir -p ${WORKDIR}/lib

# copy other dependent jar files that may be needed to run the pipeline 
# in our case the custom UDF jar from that project
COPY --from=build /udf-impl/target/*.jar ${WORKDIR}/lib/

ENV FLEX_TEMPLATE_JAVA_CLASSPATH=/template/dataflow-pipeline-1.0-SNAPSHOT.jar
ENV FLEX_TEMPLATE_JAVA_MAIN_CLASS=${MAINCLASS}