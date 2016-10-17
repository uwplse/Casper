import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Equal {
	
	public static void main(String[] args) {
		List<Integer> numbers = null;
		numbers = Arrays.asList(10, 10, 5, 10, 10);
		equal(numbers);
	}
	
	public static boolean equal(List<Integer> data) {
		boolean equal = false;
		equal = true;
		int val = 0;
		val = data.get(0);
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
					boolean flat$4 = val != flat$3;
					if (flat$4) { equal = false; }
				} else {
					break;
				}
			}
		}
		return equal;
	}
	
	public Equal() { super(); }
}
