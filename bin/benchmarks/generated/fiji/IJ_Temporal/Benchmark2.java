package generated.fiji.IJ_Temporal;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple3;

import java.util.ArrayList;
import java.util.List;

/**
 * TemporalMedian_.java: line 189
 */
public class Benchmark2 {
	public JavaRDD<Tuple3<Integer,Integer,Float>> benchmark(JavaRDD<Tuple3<Integer,Integer,Float>> rdd_0_0, int wmax) {
		return rdd_0_0.flatMap(tWinPix_i -> {
			List<Tuple3<Integer,Integer,Float>> emits = new ArrayList<>();
			if (tWinPix_i._1() != 0) emits.add(new Tuple3<Integer,Integer,Float>(tWinPix_i._1()-1,tWinPix_i._2(),tWinPix_i._3()));
			return emits.iterator();
		});
	}
}