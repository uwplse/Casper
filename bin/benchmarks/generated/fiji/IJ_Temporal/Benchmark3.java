package generated.fiji.IJ_Temporal;

import org.apache.spark.api.java.JavaPairRDD;

/**
 * TemporalMedian_.java: line 217
 */
public class Benchmark3 {
  public float benchmark(JavaPairRDD<Integer, Float> rdd_0_0) {
    return rdd_0_0.map(vec_i -> vec_i._2).reduce((v1,v2) -> v2 + v1);
  }
}