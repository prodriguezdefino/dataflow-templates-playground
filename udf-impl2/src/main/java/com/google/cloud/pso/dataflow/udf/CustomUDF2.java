package com.google.cloud.pso.dataflow.udf;

import org.apache.beam.sdk.values.Row;
import org.joda.time.DateTime;

/**
 * A custom UDF that decides on a positive sentiment based on the page score.
 */
public class CustomUDF2 implements UDF {

  private static String detailedSentiment(Integer score) {
    String sentiment = "";
    if (score < 3) {
      sentiment = "poor";
    } else if (score < 6) {
      sentiment = "okay";
    } else {
      sentiment = "great";
    }
    return sentiment;
  }

  @Override
  public Row apply(Row row) throws RuntimeException {
    return Row.fromRow(row)
            .withFieldValue("sentiment",
                    detailedSentiment(row.getInt32("page_score")))
            .withFieldValue("processing_time", DateTime.now())
            .build();
  }

}
