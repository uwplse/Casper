import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.Integer;

public class RedToMagenta {	
	public static void main(String[] args) {
		List<Integer> pixels = new ArrayList<Integer>();
		
		for (int i = 0; i < pixels.size(); i++) {
			int value = pixels.get(i);
			int red = (value >> 16) & 0xff;
			int green = (value >> 8) & 0xff;
			int blue = value & 0xff;
			pixels.set(i, (red << 16) | (green << 8) | red);
		}
	}
}
