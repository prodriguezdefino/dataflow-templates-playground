// Copyright 2020 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.pso.dataflow;

import com.google.cloud.pso.dataflow.transform.WriteToDestination;
import com.google.cloud.pso.dataflow.udf.UDF;
import com.google.gson.Gson;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Apache Beam streaming pipeline that reads JSON encoded messages from Pub/Sub, executes a configurable UDF, and writes the results to a
 * BigQuery.
 */
public class StreamingPubSubToBQ {

  private static final Logger LOG = LoggerFactory.getLogger(StreamingPubSubToBQ.class);
  private static final Gson GSON = new Gson();

  public interface Options extends StreamingOptions {

    @Description("Pub/Sub subscription to read from.")
    @Validation.Required
    String getInputSubscription();

    void setInputSubscription(String value);

    @Description("Pub/Sub subscription to read from.")
    @Default.String("")
    String getUDFClassName();

    void setUDFClassName(String value);

    @Description("Destination type for the pipeline."
            + "For BIGQUERY destination a table to "
            + "write to is expected in the form "
            + "'project:dataset.table' or 'dataset.table'."
            + "For GCS destination a gs:// location is expected.")
    @Validation.Required
    WriteToDestination.Destination getDestination();
    
    void setDestination(WriteToDestination.Destination value);
    
    @Description("URL for the pipeline's destination, "
            + "must conform the expectations for the destination.")
    @Default.String("")
    String getOutputURL();

    void setOutputURL(String value);
  }

  @DefaultCoder(AvroCoder.class)
  private static class PageReviewMessage {

    @Nullable
    String url;
    @Nullable
    Integer score;
  }

  /**
   * In charge of loading the configured UDF and execute it on every processed row.
   */
  static class ExecuteUDFDoFn extends DoFn<Row, Row> {

    private UDF udf;
    private final String className;

    public ExecuteUDFDoFn(String className) {
      this.className = className;
    }

    @Setup
    public void setup() {
      this.udf = Optional
              .ofNullable(this.className)
              .filter(cName -> !cName.isBlank())
              .map(ExecuteUDFDoFn::loadUDF)
              .orElseGet(DefaultUDF::new);
    }

    @ProcessElement
    public void process(ProcessContext context) {
      context.output(udf.apply(context.element()));
    }

    /**
     * Default implementation class if no UDF is provided.
     */
    static class DefaultUDF implements UDF {
    }

    static UDF loadUDF(String className) {
      try {
        var clazz
                = Class.forName(className, true, ExecuteUDFDoFn.class.getClassLoader());
        return (UDF) clazz.getDeclaredConstructor().newInstance();
      } catch (ClassNotFoundException
              | IllegalAccessException | IllegalArgumentException
              | InstantiationException | NoSuchMethodException
              | SecurityException | InvocationTargetException ex) {
        LOG.error(
                "Problems while loading the requested class name: " + className, ex);
        return null;
      }
    }
  }

  public static void main(final String[] args) {
    Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
    options.setStreaming(true);

    var schema = Schema.builder()
            .addStringField("url")
            .addInt32Field("page_score")
            .addNullableField("sentiment", Schema.FieldType.STRING)
            .addNullableField("processing_time", Schema.FieldType.DATETIME)
            .build();

    LOG.info("Launching pipeline to read from {}, executing UDF {} and writing to {}",
            options.getInputSubscription(),
            options.getUDFClassName(),
            options.getOutputURL());

    var pipeline = Pipeline.create(options);
    pipeline
            // Read, parse, and validate messages from Pub/Sub.
            .apply("ReadPubSub", PubsubIO.readStrings().fromSubscription(options.getInputSubscription()))
            .apply("ParseJSONToRows", MapElements.into(TypeDescriptor.of(Row.class))
                    .via(message -> {
                      // This is a good place to add error handling.
                      // The first transform should act as a validation layer to make sure
                      // that any data coming to the processing pipeline must be valid.
                      // See `MapElements.MapWithFailures` for more details.
                      var msg = GSON.fromJson(message, PageReviewMessage.class);
                      return Row.withSchema(schema)
                              .withFieldValue("url", msg.url) // row url
                              .withFieldValue("page_score", msg.score) // row page_score
                              .build();
                    })).setRowSchema(schema) // make sure to set the row schema for the PCollection
            // Add timestamps and bundle elements into windows.
            .apply("ExecuteUDF", ParDo.of(
                    new ExecuteUDFDoFn(options.getUDFClassName()))).setRowSchema(schema)
            // Write to the configured destination
            .apply("WriteToDestination", WriteToDestination
                    .create(options.getDestination(), options.getOutputURL()));

    // For a Dataflow Flex Template, do NOT waitUntilFinish().
    pipeline.run();
  }
}
