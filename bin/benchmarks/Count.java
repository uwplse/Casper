import java.util.List;
import java.lang.Integer;

public class Count {	
	public static void main(String[] args) {
		countList(null);
	}
	
	public static int countList(List<Integer> data) {
		int count = 0;
		for(int i=0; i<data.size(); i++) {
			count++;
		}
		return count;
	}
}
