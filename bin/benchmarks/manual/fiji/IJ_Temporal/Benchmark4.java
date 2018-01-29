package manual.fiji.IJ_Temporal;

import org.apache.spark.api.java.JavaPairRDD;

/**
 * TemporalMedian_.java: line 221
 */
public class Benchmark4 {
	public float benchmark(JavaPairRDD<Integer, Float> vec, float mean) {
		return vec.values().map(v -> (mean - v) * (mean - v)).reduce((a, b) -> a + b);
	}
}