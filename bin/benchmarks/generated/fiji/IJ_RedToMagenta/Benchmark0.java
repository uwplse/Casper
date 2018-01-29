package generated.fiji.IJ_RedToMagenta;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple3;

/**
* Convert_Red_To_Magenta.java: line 37
*/
public class Benchmark0 {
	public JavaRDD<Tuple3<Integer,Integer,Integer>> benchmark(JavaRDD<Tuple3<Integer,Integer,Integer>> rdd_0_0, int w, int h) {
		return rdd_0_0.map(pixels_i -> new Tuple3<Integer,Integer,Integer>(pixels_i._1(), pixels_i._2(), (((pixels_i._3() >> 16) & 0xff) << 16) | (((pixels_i._3() >> 8) & 0xff) << 8) | ((pixels_i._3() >> 16) & 0xff)));
	}
}