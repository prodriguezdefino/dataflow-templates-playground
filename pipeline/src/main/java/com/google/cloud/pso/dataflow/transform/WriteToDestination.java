package com.google.cloud.pso.dataflow.transform;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import java.util.Arrays;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ToJson;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.beam.sdk.values.Row;
import org.joda.time.Duration;

/**
 *
 */
public class WriteToDestination extends PTransform<PCollection<Row>, PDone> {

  public enum Destination {
    BIGQUERY,
    GCS
  }

  private final Destination destination;
  private final String destinationURL;

  private WriteToDestination(Destination dest, String url) {
    this.destination = dest;
    this.destinationURL = url;
  }

  public static WriteToDestination create(Destination dest, String url) {
    return new WriteToDestination(dest, url);
  }

  @Override
  public PDone expand(PCollection<Row> input) {
    switch (this.destination) {
      case BIGQUERY: {
        expandWriteOnBQ(input);
        break;
      }
      case GCS: {
        expandWriteOnGCS(input);
        break;
      }
    }
    return PDone.in(input.getPipeline());
  }

  private void expandWriteOnBQ(PCollection<Row> input) {
    input.apply("WriteToBQ", BigQueryIO
            .<Row>write()
            .to(this.destinationURL)
            .useBeamSchema()
            .withMethod(BigQueryIO.Write.Method.STORAGE_API_AT_LEAST_ONCE)
            .withSchema(new TableSchema().setFields(Arrays.asList(
                    new TableFieldSchema().setName("url").setType("STRING"),
                    new TableFieldSchema().setName("page_score").setType("INTEGER"),
                    new TableFieldSchema().setName("sentiment").setType("STRING"),
                    new TableFieldSchema().setName("processing_time").setType("TIMESTAMP"))))
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND));
  }

  private void expandWriteOnGCS(PCollection<Row> input) {
    input
            .apply("1mWindow", Window
                    .<Row>into(FixedWindows.of(Duration.standardMinutes(1L)))
                    .withAllowedLateness(Duration.standardMinutes(1L))
                    .discardingFiredPanes())
            .apply("ToJSON", ToJson.of())
            .apply("WriteToGCS", TextIO
                    .write()
                    .to(this.destinationURL)
                    .withWindowedWrites());
  }

}
