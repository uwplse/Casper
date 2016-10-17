import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class EqualFrequency {
	
	public static void main(String[] args) {
		List<Integer> numbers = null;
		numbers = Arrays.asList(100, 110, 55, 110, 100);
		equalFrequency(numbers);
	}
	
	public static boolean equalFrequency(List<Integer> data) {
		int first = 0;
		first = 0;
		int second = 0;
		second = 0;
		{
			int i = 0;
			i = 0;
			boolean loop$0 = false;
			loop$0 = false;
			while (true) {
				if (loop$0) {
					{
						int flat$1 = i + 1;
						i = (int) flat$1;
					}
					;
				}
				int flat$2 = data.size();
				loop$0 = i < flat$2;
				if (loop$0) {
					int var = 0;
					var = data.get(i);
					boolean flat$3 = var == 100;
					if (flat$3) {
						{
							int flat$4 = first + 1;
							first = (int) flat$4;
						}
						;
					}
					boolean flat$5 = var == 110;
					if (flat$5) {
						{
							int flat$6 = second + 1;
							second = (int) flat$6;
						}
						;
					}
				} else {
					break;
				}
			}
		}
		boolean flat$7 = first == second;
		return flat$7;
	}
	
	public EqualFrequency() { super(); }
}
