package manual.fiji.IJ_RedToMagenta;

import org.apache.spark.api.java.JavaRDD;

/**
* Convert_Red_To_Magenta.java: line 37
*/
public class Benchmark0 {
	class Pixel {
		int row;
		int col;
		int val;
		public Pixel(int r, int c, int v) {
			row = r;
			col = c;
			val = v;
		}
	}

	public JavaRDD<Pixel> benchmark(JavaRDD<Pixel> pixels, int w, int h) {
		return pixels.map(pixel -> {
			int value = pixel.val;
			int red = (value >> 16) & 0xff;
			int green = (value >> 8) & 0xff;
			int blue = value & 0xff;
			if (false && blue > 16)
				return pixel;
			else
				return new Pixel(pixel.row, pixel.col, (red << 16) | (green << 8) | red);
		});
	}
}