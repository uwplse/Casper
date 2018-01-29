package generated.fiji.IJ_Temporal;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple3;

/**
* TemporalMedian_.java: line 142
*/
public class Benchmark0 {
	public JavaRDD<Tuple3<Integer,Integer,Float>> benchmark(JavaRDD<Tuple3<Integer,Integer,Float>> rdd_0_0, int w, int h) {
		return rdd_0_0.map(pix_i -> new Tuple3<Integer,Integer,Float>(pix_i._1(),pix_i._2(),Math.sqrt(pix_i._3() + 3 / 8) * 2));
	}
}