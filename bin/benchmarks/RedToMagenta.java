package fiji.redtomagenta;

import java.util.ArrayList;
import java.util.List;

public class RedToMagentaList {
	public static void main(String[] args) throws Exception {
		int w = 100;
		int h = 100;
		List<Integer> pixels = new ArrayList<Integer>(w*h);
		
		//for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				int value = pixels.get(i);
				int redAndGreen = (value) & 0xffff00;
				int red = (value >> 16) & 0xff;
				pixels.set(i, redAndGreen | red);
			}
		//}
	}
}
