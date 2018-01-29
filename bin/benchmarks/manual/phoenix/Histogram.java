package manual.phoenix;

import org.apache.spark.api.java.JavaRDD;

class Histogram {
	
	public static class Pixel {
		public int r, g, b;
	
		public Pixel(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	
	public static int[][] histogram(JavaRDD<Pixel> image, int[] hR, int[] hG, int[] hB) {
		return image.aggregate(new int[3][256],
					(res, pixel) -> {
						res[0][pixel.r]++;
						res[1][pixel.g]++;
						res[2][pixel.b]++;
						return res;
					},
					(res1, res2) -> {
						for (int i=0; i<256; i++) {
							res1[0][i] = res1[0][i] + res2[0][i];
							res1[1][i] = res1[1][i] + res2[1][i];
							res1[2][i] = res1[2][i] + res2[2][i];
						}
						return res1;
					}
		);
	}

}
