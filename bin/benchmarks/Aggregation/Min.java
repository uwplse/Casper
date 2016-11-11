import java.util.List;
import java.lang.Integer;
import java.lang.Math;

public class Min {	
	public static void main(String[] args) {
		minList(null);
	}
	
	public static int minList(List<Integer> data) {
		int min = 0;
		for(int i=0; i<data.size(); i++) {
			int var = data.get(i);
			min = Math.min(var,min);
		}
		return min;
	}
}n
