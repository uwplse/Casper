package manual.fiji.IJ_Temporal;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

/**
 * TemporalMedian_.java: line 169
*/
public class Benchmark1 {
	class Pixel {
		int frame;
		int pos;
		float val;
		public Pixel(int f, int p, float v) {
			frame = f;
			pos = p;
			val = v;
		}
	}

	public JavaPairRDD<Integer,Float> benchmark(JavaRDD<Pixel> tWinPix, int v, int wmin, int wmax) {
		return tWinPix.filter(pixel -> pixel.pos==v).mapToPair(pixel -> new Tuple2(pixel.frame, pixel.val));
	}
}