import java.util.List;
import java.lang.Integer;
import java.lang.Math;

public class Delta {	
	public static void main(String[] args) {
		deltaList(null);
	}
	
	public static int deltaList(List<Integer> data) {
		int max = 0;
		int min = 0;
		for(int i=0; i<data.size(); i++) {
			int var = data.get(i);
			max = Math.max(var,max);
			min = Math.min(var,min);
		}
		return max-min;
	}
}
