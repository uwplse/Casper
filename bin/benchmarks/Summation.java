import java.util.List;
import java.lang.Integer;

public class SummationArray {	
	public static void main(String[] args) {
		sumList(null);
	}
	
	public static int sumList(List<Integer> data) {
		int sum = 0;
		for(int i=0; i<data.size(); i++) {
			sum += data.get(i);
		}
		return sum;
	}
}
