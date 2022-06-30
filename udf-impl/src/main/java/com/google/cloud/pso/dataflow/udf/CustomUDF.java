package com.google.cloud.pso.dataflow.udf;

import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CustomUDF implements UDF {

  private static final Logger LOG = LoggerFactory.getLogger(CustomUDF.class);

  @Override
  public Row apply(Row row) throws RuntimeException {
    LOG.info("just logging for now");
    return UDF.super.apply(row); 
  }

}
