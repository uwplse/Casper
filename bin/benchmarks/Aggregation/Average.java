import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Average {	
	public static void main(String[] args) {
		List<Integer> numbers = Arrays.asList(10, 5, 7, 12, 3);
		avgList(numbers);
	}
	
	public static int avgList(List<Integer> data) {
		int sum = 0;
		int count = 0;
		for(int i=0; i<data.size(); i++) {
			sum += data.get(i);
			count++;
		}
		return sum / count;
	}
}
