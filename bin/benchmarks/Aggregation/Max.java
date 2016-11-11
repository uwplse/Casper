import java.util.List;
import java.lang.Integer;
import java.lang.Math;

public class Max {	
	public static void main(String[] args) {
		maxList(null);
	}
	
	public static int maxList(List<Integer> data) {
		int max = Integer.MIN_VALUE;
		for(int i=0; i<data.size(); i++) {
			int var = data.get(i);
			max = Math.max(var,max);
		}
		return max;
	}
}
