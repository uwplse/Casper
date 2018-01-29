package manual.fiji.IJ_Trails;

import org.apache.spark.api.java.JavaPairRDD;

/**
 * Trails_.java: line 160
 */
public class Benchmark2 {
  public float benchmark(JavaPairRDD<Integer, Float> vec) {
    return vec.values().reduce((a, b) -> a + b);
  }
}