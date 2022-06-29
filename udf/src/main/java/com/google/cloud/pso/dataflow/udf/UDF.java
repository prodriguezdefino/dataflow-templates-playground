package com.google.cloud.pso.dataflow.udf;

import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.Row;

/**
 * Represents a custom function that can be implemented to transform a Beam Row object.
 */
public interface UDF extends SerializableFunction<Row, Row> { 

  /**
   * Function that applies the transformation to the input object. By default this method returns the input object as-is. 
   * @param row The input row
   * @return the transformed row
   * @throws RuntimeException If errors occur while executing the transformation.
   */
  @Override
  default Row apply(Row row) throws RuntimeException {
    return row;
  }
}
