package manual.fiji.IJ_Trails;

import org.apache.spark.api.java.JavaRDD;

/**
 * Trails_.java: line 169
 */
public class Benchmark3 {
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

	public JavaRDD<Pixel> benchmark(JavaRDD<Pixel> tWinPix, int wmax) {
		return tWinPix.filter(pixel -> pixel.frame!=0).map(pixel -> new Pixel(pixel.frame-1,pixel.pos,pixel.val));
	}
}