package com.google.cloud.pso.dataflow.transform;

import com.google.gson.Gson;
import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.apache.beam.sdk.values.TypeDescriptor;

/**
 *
 */
public class ReadFromSource extends PTransform<PBegin, PCollection<Row>> {

  private static final Gson GSON = new Gson();

  public enum Source {
    PUBSUB,
    GCS
  }

  @DefaultCoder(AvroCoder.class)
  static class PageReviewMessage {

    @Nullable
    String url;
    @Nullable
    Integer page_score;
  }

  private final Source source;
  private final String sourceURL;
  private final Schema schema;

  private ReadFromSource(Source source, String url, Schema schema) {
    this.source = source;
    this.sourceURL = url;
    this.schema = schema;
  }

  public static ReadFromSource create(Source source, String url, Schema schema) {
    return new ReadFromSource(source, url, schema);
  }

  @Override
  public PCollection<Row> expand(PBegin input) {
    PCollection<Row> read = null;
    switch (this.source) {
      case PUBSUB: {
        read = expandReadFromPubSub(input);
        break;
      }
      case GCS: {
        read = expandReadFromGCS(input);
        break;
      }
    }
    return read;
  }

  private PCollection<Row> transformToRowPCollection(PCollection<String> input) {
    return input.apply("ParseJSONToRows", MapElements.into(TypeDescriptor.of(Row.class))
            .via(message -> {
              // This is a good place to add error handling.
              // The first transform should act as a validation layer to make sure
              // that any data coming to the processing pipeline must be valid.
              // See `MapElements.MapWithFailures` for more details.
              var msg = GSON.fromJson(message, PageReviewMessage.class);
              return Row.withSchema(schema)
                      .withFieldValue("url", msg.url) // row url
                      .withFieldValue("page_score", msg.page_score) // row page_score
                      .build();
            })).setRowSchema(schema); // make sure to set the row schema for the PCollection
  }

  private PCollection<Row> expandReadFromPubSub(PBegin input) {
    return transformToRowPCollection(
            // Read from Pub/Sub.
            input.apply("ReadFromPubSub", PubsubIO
                    .readStrings()
                    .fromSubscription(this.sourceURL)));
  }

  private PCollection<Row> expandReadFromGCS(PBegin input) {
    return transformToRowPCollection(
            // Read from GCS location
            input.apply(TextIO.read().from(this.sourceURL)));
  }

}
