package manual.fiji.IJ_Temporal;

import org.apache.spark.api.java.JavaPairRDD;

/**
 * TemporalMedian_.java: line 217
 */
public class Benchmark3 {
  public float benchmark(JavaPairRDD<Integer, Float> vec) {
    return vec.values().reduce((a, b) -> a + b);
  }
}