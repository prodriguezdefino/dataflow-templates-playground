package com.google.cloud.pso.dataflow.udf;

import java.util.Optional;
import org.apache.beam.sdk.values.Row;
import org.joda.time.Instant;

/**
 * A custom UDF that decides on a positive sentiment based on the page score. 
 */
public class CustomUDF implements UDF {

  @Override
  public Row apply(Row row) throws RuntimeException {
    
    return Row.fromRow(row)
            .withFieldValue("sentiment", 
                    Optional.of(row.getInt32("page_score"))
                            .map(score -> score > 5 ? "positive" : "negative").get())
            .withFieldValue("processing_time", Instant.now().toString())
            .build();
  }

}
