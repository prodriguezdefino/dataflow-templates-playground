FROM adoptopenjdk/maven-openjdk11:latest AS build

RUN mkdir /build
ADD . /build
WORKDIR /build
USER root
RUN mvn clean package -Dmaven.test.skip=true

#
# Package stage
#
FROM gcr.io/dataflow-templates-base/java11-template-launcher-base:latest

ARG WORKDIR=/template
ARG MAINCLASS=com.google.cloud.pso.dataflow.ProcessingPipeline
RUN mkdir -p ${WORKDIR}
WORKDIR ${WORKDIR}

# Bring the bundled jar file for the pipeline from the build container
COPY --from=build /build/pipeline/target/pipeline-bundled-1.0-SNAPSHOT.jar /template/
RUN mkdir -p ${WORKDIR}/lib

# copy other dependent jar files that may be needed to run the pipeline 
# in our case the custom UDF jar from that project, 
# this could be downloaded from an artifactory as needed
COPY --from=build /build/udf-impl/target/udf-impl-1.0-SNAPSHOT.jar ${WORKDIR}/lib/

ENV FLEX_TEMPLATE_JAVA_CLASSPATH=/template/pipeline-bundled-1.0-SNAPSHOT.jar:/template/lib/udf-impl-1.0-SNAPSHOT.jar
ENV FLEX_TEMPLATE_JAVA_MAIN_CLASS=${MAINCLASS}
