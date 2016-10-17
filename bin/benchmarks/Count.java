import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Count {	
	public static void main(String[] args) {
		List<Integer> numbers = Arrays.asList(10, 5, 7, 12, 3);
		countList(numbers);
	}
	
	public static int countList(List<Integer> data) {
		int count = 0;
		for(int i=0; i<data.size(); i++) {
			int val = data.get(i);
			count++;
		}
		return count;
	}
}
