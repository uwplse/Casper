import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Count {
	
	public static void main(String[] args) {
		List<Integer> numbers = null;
		numbers = Arrays.asList(10, 5, 7, 12, 3);
		countList(numbers);
	}
	
	public static int countList(List<Integer> data) {
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
					int val = 0;
					val = data.get(i);
					{
						int flat$3 = count + 1;
						count = (int) flat$3;
					}
					;
				} else {
					break;
				}
			}
		}
		return count;
	}
	
	public Count() { super(); }
}
