package manual.fiji.IJ_Temporal;

import org.apache.spark.api.java.JavaRDD;

/**
* TemporalMedian_.java: line 142
*/
public class Benchmark0 {
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

	public JavaRDD<Pixel> benchmark(JavaRDD<Pixel> pixels, int w, int h) {
		return pixels.map(pixel -> {
			double transf = 2 * Math.sqrt(pixel.val + 3 / 8);
			return new Pixel(pixel.frame, pixel.pos, (float)transf);
		});
	}
}