import java.util.List;
import java.util.Arrays;

class HistogramJava {
	
	public static class Pixel {
		public int r, g, b;
	
		public Pixel(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	
	public static void main(String[] args) {
		List<Pixel> pixels = Arrays.asList(new Pixel(10, 10, 10), new Pixel(120, 120, 120), new Pixel(210, 210, 210), new Pixel(10, 120, 210));
		int[] hR = new int[256];
		int[] hG = new int[256];
		int[] hB = new int[256];
		histogram(pixels, hR, hG, hB);
	}

	@SuppressWarnings("unchecked")
	public static int[][] histogram(List<Pixel> image, int[] hR, int[] hG, int[] hB) {
		for (int i = 0; i < image.size(); i += 1) {
			int r = image.get(i).r;
			int g = image.get(i).g;
			int b = image.get(i).b;
			hR[r]++;
			hG[g]++;
			hB[b]++;
		}

		int[][] result = new int[3][];
		result[0] = hR;
		result[1] = hG;
		result[2] = hB;

		return result;
	}

}
