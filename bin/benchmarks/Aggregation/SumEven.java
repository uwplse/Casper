import java.util.List;
import java.lang.Integer;

public class SumEven {	
	public static void main(String[] args) {
		sumList(null);
	}
	
	public static int sumList(List<Integer> data) {
		int sum = 0;
		for(int i=0; i<data.size(); i++) {
			int var = data.get(i);
			if(var % 2 == 0){
				sum += var;
			}
		}
		return sum;
	}
}
