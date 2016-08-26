import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class EqualFrequency {	
	public static void main(String[] args) {
		List<Integer> numbers = Arrays.asList(100, 110, 55, 110, 100);
		equalFrequency(numbers);
	}
	
	public static boolean equalFrequency(List<Integer> data) {
		int first = 0;
		int second = 0;
		for(int i=0; i<data.size(); i++) {
			int var = data.get(i);
			if(var == 100){
				first++;
			}
			if(var == 110){
				second++;
			}
		}
		return first == second;
	}
}
