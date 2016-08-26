import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Equal {	
	public static void main(String[] args) {
		List<Integer> numbers = Arrays.asList(10, 10, 5, 10, 10);
		equal(numbers);
	}
	
	public static boolean equal(List<Integer> data) {
		boolean equal = true;
		int val = data.get(0);
		for(int i=0; i<data.size(); i++) {
			if(val != data.get(i)){
				equal = false;
			}
		}
		return equal;
	}
}
