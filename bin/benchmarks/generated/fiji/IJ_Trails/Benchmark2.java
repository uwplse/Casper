package generated.fiji.IJ_Trails;

import org.apache.spark.api.java.JavaPairRDD;

/**
 * Trails_.java: line 160
 */
public class Benchmark2 {
  public float benchmark(JavaPairRDD<Integer, Float> rdd_0_0) {
    return rdd_0_0.map(tvec_i -> tvec_i._2).reduce((v1,v2) -> v2 + v1);
  }
}