import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Average {
	
	public static void main(String[] args) {
		List<Integer> numbers = null;
		numbers = Arrays.asList(10, 5, 7, 12, 3);
		avgList(numbers);
	}
	
	public static int avgList(List<Integer> data) {
		int sum = 0;
		sum = 0;
		int count = 0;
		count = 0;
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
					Integer flat$3 = data.get(i);
					int flat$4 = sum + flat$3;
					sum = (int) flat$4;
					{
						int flat$5 = count + 1;
						count = (int) flat$5;
					}
					;
				} else {
					break;
				}
			}
		}
		int flat$6 = sum / count;
		return flat$6;
	}
	
	public Average() { super(); }
}
