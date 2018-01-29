package generated.fiji.IJ_Temporal;

import org.apache.spark.api.java.JavaPairRDD;

/**
 * TemporalMedian_.java: line 221
 */
public class Benchmark4 {
	public float benchmark(JavaPairRDD<Integer, Float> rdd_0_0, float mean) {
		return rdd_0_0.map(vec_i -> (vec_i._2 - mean) * (vec_i._2 - mean)).reduce((v1,v2) -> v1 + v2);
	}
}