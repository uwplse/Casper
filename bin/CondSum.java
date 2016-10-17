import java.util.List;
import java.lang.Integer;

public class ConditionalSum {
	
	public static void main(String[] args) { sumList(null); }
	
	public static int sumList(List<Integer> data) {
		int sum = 0;
		sum = 0;
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
					boolean flat$3 = var < 100;
					if (flat$3) {
						int flat$4 = sum + var;
						sum = (int) flat$4;
					}
				} else {
					break;
				}
			}
		}
		return sum;
	}
	
	public ConditionalSum() { super(); }
}
